package com.na.light;

import java.io.Serializable;

/**
 * 注册中心节点数据。
 * Created by sunny on 2017/8/7 0007.
 */
public class LightServiceNodeData implements Serializable {
    private final static long serialVersionUID = 7297706850337329870L;

    private String contextPath;
    private String group;
    private String serviceName;
    private int version;
    private String token;

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        if(contextPath !=null && contextPath.trim().length()>=0) {
            this.contextPath = contextPath.trim();
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        if(group !=null && group.trim().length()>=0) {
            this.group = group.trim();
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        if(token !=null && token.trim().length()>=0) {
            this.token = token.trim();
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        if(serviceName!=null && serviceName.trim().length()>=0) {
            this.serviceName = serviceName.trim();
        }
    }
}
