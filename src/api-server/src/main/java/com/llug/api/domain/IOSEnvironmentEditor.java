package com.llug.api.domain;

import java.beans.PropertyEditorSupport;

import com.wch.commons.utils.Utils;

public class IOSEnvironmentEditor extends PropertyEditorSupport {
    @Override
    public String getAsText() {
        IOSEnvironment env = (IOSEnvironment) getValue();
        return env.toString();
    }

    @Override
    public void setAsText(String s) {
        setValue(Utils.safeParseEnum(IOSEnvironment.class, s));
    }
}
