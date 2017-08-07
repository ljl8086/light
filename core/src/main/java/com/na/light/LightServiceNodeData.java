package com.na.light;

import java.io.Serializable;

/**
 * 注册中心节点数据。
 * Created by sunny on 2017/8/7 0007.
 */
public class LightServiceNodeData implements Serializable {
    private String projectName;
    private String interfaceName;
    private int version;
    private String password;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        if(projectName!=null && projectName.trim().length()>=0) {
            this.projectName = projectName.trim();
        }
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        if(interfaceName!=null && interfaceName.trim().length()>=0) {
            this.interfaceName = interfaceName.trim();
        }
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if(password!=null && password.trim().length()>=0) {
            this.password = password.trim();
        }
    }
}
