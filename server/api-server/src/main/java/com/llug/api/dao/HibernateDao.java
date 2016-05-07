package com.llug.api.dao;

import static ch.lambdaj.Lambda.closure;
import static ch.lambdaj.Lambda.of;
import static ch.lambdaj.Lambda.var;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ch.lambdaj.function.closure.Closure;

import com.google.common.base.Preconditions;
import com.llug.api.persistence.model.EntityObject;
import com.wch.commons.utils.ArgumentException;
import com.wch.commons.utils.Utils;

@Transactional
@Component
public class HibernateDao {
    @Autowired
    @Qualifier("sessionFactory")
    private SessionFactory sessionFactory;

    public <T extends EntityObject> List<T> query(Class<T> clazz, final Object... params) {
        if (params.length % 2 != 0) {
            throw new ArgumentException("Number of params must be even");
        }

        final Session session = getCurrentSession();
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("FROM %s WHERE ", clazz.getName()));

        int paramIndex = 1;
        for (int i = 0; i < params.length; i += 2, paramIndex++) {
            if (i != 0) {
                sb.append("AND ");
            }

            sb.append(String.format("%s = :value%d", params[i], paramIndex));
        }

        Query query = session.createQuery(sb.toString());

        // http://www.mkyong.com/hibernate/hibernate-query-examples-hql/
        paramIndex = 1;
        for (int i = 0; i < params.length; i += 2, paramIndex++) {
            String value = String.format("value%d", paramIndex);
            query.setParameter(value, params[i + 1]);
        }

        @SuppressWarnings("unchecked")
        List<T> results = query.list();

        return results;
    }

    public <T extends EntityObject> T find(Class<T> clazz, final long id) {
        Session session = getCurrentSession();

        try {
            Object o = session.get(clazz, id);
            return (T) o;
        } finally {
            session.close();
        }
    }

    public final <T extends EntityObject> T findOne(Class<T> clazz, final long id) {
        return (T) getCurrentSession().get(clazz, id);
    }

    public final <T extends EntityObject> List<EntityObject> findAll() {
        return getCurrentSession().createQuery("from " + EntityObject.class).list();
    }

    public <T extends EntityObject> void create(final T entity) {
        Session session = null;
        try {
            Preconditions.checkNotNull(entity);
            // getCurrentSession().persist(entity);
            session = getCurrentSession();
            session.saveOrUpdate(entity);
        } finally {
            //session.close();
        }
    }

    public <T extends EntityObject> void create(List<EntityObject> objs) {
        Closure cl = closure();
        {
            of(this).create(var(EntityObject.class));
        }

        cl.each(objs);
    }

    public final <T extends EntityObject> EntityObject update(final EntityObject entity) {
        Preconditions.checkNotNull(entity);
        return (EntityObject) getCurrentSession().merge(entity);

        /*
        Session session = sessionFactory.openSession();
        try {
            session.merge(entity);
        } finally {
            session.close();
        }
        */
    }

    public <T extends EntityObject> void update(List<EntityObject> objs) {
        Closure cl = closure();
        {
            of(this).update(var(EntityObject.class));
        }

        cl.each(objs);
    }

    public final void delete(final EntityObject entity) {
        Preconditions.checkNotNull(entity);
        getCurrentSession().delete(entity);
    }

    public final <T extends EntityObject> void deleteById(Class<T> clazz, final long entityId) {
        final EntityObject entity = findOne(clazz, entityId);
        Preconditions.checkState(entity != null);
        delete(entity);
    }

    public <T extends EntityObject> Session getCurrentSession() {
        Session session = null;
        try {
            session = sessionFactory.getCurrentSession();
            if (!session.isOpen()) {
                session = sessionFactory.openSession();
            }
        } catch (org.hibernate.HibernateException he) {
            session = sessionFactory.openSession();
        }

        return session;
    }

    @SuppressWarnings("unchecked")
    public <T extends EntityObject> boolean dynamicUpdate(final String hibernateQueryFragment,
            final String parameter,
            final Object parameterValue,
            final IDynamicUpdateImpl dynamicUpdateImpl) {
        // http://www.mkyong.com/hibernate/hibernate-dynamic-update-attribute-example/
        final Query query = getCurrentSession().createQuery(hibernateQueryFragment);
        query.setParameter(parameter, parameterValue);

        final List<T> list = query.list();
        final T entityObj = !Utils.isNullOrEmpty(list) ? list.get(0) : null;

        if (entityObj != null) {
            dynamicUpdateImpl.setParameters(entityObj);

            getCurrentSession().update(entityObj);

            return true;
        } else {
            return false;
        }
    }

    public interface IDynamicUpdateImpl {
        <T extends EntityObject> void setParameters(T t);
    }
}