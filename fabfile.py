import os

from datetime import datetime
from fabric.api import run, sudo, env, cd, local, prefix, put, lcd, settings
from fabric.contrib.files import exists
from fabric.contrib.project import rsync_project

import boto
from boto.s3.key import Key

user = 'deploy'
backends_dir = '/var/backends'
dist = 'debian'

release_bucket_name = 'llug-release'

# local
tmp_dir = '/tmp/backends_tmp_' + str(os.getpid())
commons_src_dir = 'src/commons'
api_server_src_dir = 'src/api-server'
host_count = len(env.hosts)

javadoc_dir = ''

def set_user_dir():
   global dist, user, backends_dir, javadoc_dir
   with settings(warn_only=True):
       issue = run('id ubuntu').lower()
       if 'id: ubuntu' in issue:
            dist = 'centos'
       elif 'uid=' in issue:
            dist = 'ubuntu'
            user = 'ubuntu'
            backends_dir = '/mnt/llug'
            javadoc_dir = backends_dir + '/docs/'
   print dist,user,backends_dir,javadoc_dir

def clean_local():
    local("rm -rf %s" % (tmp_dir))


def prepare_remote_dirs():
    set_user_dir();
    if not exists(backends_dir):
        sudo('mkdir -p %s' % backends_dir)
        sudo('chown %s %s' % (user, backends_dir))

def mkdir_chown(dir):
    set_user_dir();
    sudo("mkdir -p %s" % (dir))
    sudo("chown -R %s %s" % (user, dir))

def start_on_boot(name, dist):
    if dist == 'debian':
        sudo('update-rc.d %s defaults' % name)
    elif dist == 'ubuntu':
        sudo('update-rc.d %s defaults' % name)
    elif dist == 'centos':
        sudo('/sbin/chkconfig --level 3 %s on' % name)
    else:
        raise ValueError('dist can only take debian, centos')

def deploy_api_service():
    global host_count
    set_user_dir();

    # uncomment if you are not using jenkins maven project; otherwise this is a double compile
    #if (host_count == len(env.hosts)):
    #   local("cd %s && mvn clean compile package install -Dmaven.test.skip=true" % (commons_src_dir))
    #   local("cd %s && mvn clean compile package" % (api_server_src_dir))

    local("mkdir -p %s/llug/api-server/conf" % (tmp_dir))
    local("mkdir -p %s/llug/api-server/resources" % (tmp_dir))

    # copy api-server code
    local("cp -v %s/target/*.jar %s/llug/api-server" % (api_server_src_dir, tmp_dir))
    local("cp -rv %s/src/main/config/* %s/llug/api-server/conf" % (api_server_src_dir, tmp_dir))
    local("cp -v %s/src/main/config/api-server.xml %s/llug/api-server/conf" % (api_server_src_dir, tmp_dir))
    local("cp -rv %s/src/main/webapp %s/llug/api-server/webapp" % (api_server_src_dir, tmp_dir))
    local("cp -rv %s/target/dependencies %s/llug/api-server" % (api_server_src_dir, tmp_dir))
    local("cp -rv %s/src/main/resources/* %s/llug/api-server/resources" % (api_server_src_dir, tmp_dir))
    local("cp -rv %s/src/main/config/* %s/llug/api-server/conf" % (api_server_src_dir, tmp_dir))


    # publish java docs
    #if len(javadoc_dir) != 0:
    #   local("cp -rv %s/target/apidocs/* %s" % (api_server_src_dir, javadoc_dir))

    # prepare remote
    prepare_remote_dirs()

    # push files
    rsync_project(local_dir=tmp_dir + "/llug/api-server/", remote_dir=backends_dir + "/api-server", delete=True)

    sudo("chown -R %s:daemon %s" % (user, backends_dir))
    put('deploy/init.d/api-server', '/etc/init.d/api-server', use_sudo=True, mode=0544)

    # restart services
    sudo("/etc/init.d/api-server restart")

    start_on_boot('api-server', dist)
    host_count -= 1
    if (host_count == 0):
       #local("cd %s && tar -czf api-server-build-current.gz llug/api-server" % (tmp_dir))
       #upload_product("%s/api-server-build-current.gz"%tmp_dir, release_bucket_name)
       clean_local()

def upload_product(product, bucket_name):
    s3conn = boto.connect_s3()
    s3bucket = s3conn.get_bucket(bucket_name)

    """ Upload the build artifacts to S3 for deployment. """
    if not os.path.exists(product):
        raise Exception('Build product %s does not exist, aborting.' % product)
    (path, sep, filename) = product.rpartition('/')
    print "Upload", filename, "to S3"
    s3key = Key(s3bucket)
    s3key.key = filename
    s3key.set_contents_from_filename(product, replace=True)
    s3key.close()
