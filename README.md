# Blah_Plugin

## 使用场景:
公司项目在使用Mybatis的时候要用到工具类
```java
import DBUtils;
public List<T> getXxxx() {
    DBUtils.selectList("testMapper.selectId", param);
}
```
反向找到使用了这个sql的DBUtils
```xml
<mapper namespace="testMapper" >
    <select id="selectById">
        select * from a_table where 1=1
    </select>
</mapper>
```
