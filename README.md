# Blah_Plugin

## 使用场景:
公司项目在使用Mybatis的时候要用到工具类
```java
import DBUtils;
public List<T> getXxxx() {
    DBUtils.selectList("testMapper.selectId", param);
}
```
在Mybatis Helper中配置项目的数据库工具类

可以找到项目中引用该sqlId的地方
```xml
<mapper namespace="testMapper" >
    <select id="selectById">
        select * from a_table where 1=1
    </select>
</mapper>
```
