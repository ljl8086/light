# light
轻量级的远程通信组件

指南
服务端
  1. 导入依赖
```
<dependency>
  <groupId>com.na.light</groupId>
  <artifactId>spring-boot-starter-light-server</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```
  2. 标记远程接口服务(必须要有接口，该标记标注在实现类上)
  ```
  @LightRpcService("userRemote")
public class UserRemoteImpl implements IUserRemote {
  ```
  3. 增加相应配置
  ```
spring.light.scan=com.na.manager.remote   ##远程接口目录
spring.light.zookeeper=192.168.0.238:2181
  ```
  
  4. 启动服务
  
  
  客户端
  1. 导入依赖
```
<dependency>
  <groupId>com.na.light</groupId>
  <artifactId>spring-boot-starter-light-client</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```
  2. 标记远程接口服务(必须要有接口，该标记标注在实现类上)
  ```
@LightRpcService("userRemote")
public interface IUserRemote {
  ```
  3. 增加相应配置
  ```
spring.light.scan=com.na.manager.remote   ##远程接口目录
spring.light.zookeeper=192.168.0.238:2181
  ```
  4. 调用服务
  ```
    @Autowired
    private IUserRemote userRemote;
  ```
  4. 启动服务
