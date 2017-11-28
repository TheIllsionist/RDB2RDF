/**
* 查询数据库中每张实体表的主键列表和数据类型
* 结果保存在 HashMap<String,Column> entityKey =new HashMap<>(),其中键为表名,值为主键字段
*/
--方式1:最快
select table_name,column_name,data_type
from information_schema.columns
where table_schema = 'task863'
  and column_key = 'pri'
  and table_name not in
     (select table_name
      from information_schema.key_column_usage
      where table_schema = 'task863'
        and referenced_table_name is not null)

--方式二:慢
select table_name,column_name,data_type 
from information_schema.columns as cols 
where table_schema = 'task863' 
  and column_key = 'pri' 
  and not exists 
  (select 1 
   from information_schema.key_column_usage 
   where table_schema = 'task863' 
     and table_name = cols.table_name 
     and referenced_table_name is not null)

/**
* 查询库中所有实体表的所有字段和其数据类型
* 结果保存在HashMap<String>
*/
--方式一:最快
select table_name,column_name,data_type
from information_schema.columns
where table_schema = 'task863'
  and table_name in
  (select table_name 
   from information_schema.key_column_usage 
   where table_schema = 'task863'
     and table_name not in 
        (select table_name 
         from information_schema.key_column_usage 
         where table_schema = 'task863' 
         and referenced_table_name is not null))


--第一次查询用这个
***********************************************************************************
/**
* 查询数据库中每一张实体表的表名,字段名,数据类型,键类型(只需区分是否为 pri)
* 查询一次,处理结果时分开
*/
select table_name,column_name,data_type,column_key
from information_schema.columns
where table_schema = 'task863'
  and table_name in 
  (select distinct table_name 
   from information_schema.key_column_usage 
   where table_schema = 'task863'
     and table_name not in 
     (select table_name 
      from information_schema.key_column_usage 
      where table_schema = 'task863' 
        and referenced_table_name is not null))
  --HashMap<String,List<Column>> entityKey =new HashMap<>(),其中键为表名,值为字段列表
**************************************************************************************



***************************************************************************************
/**
* 查询库中的所有引用信息
* 
*/
select table_name,column_name,referenced_table_name,referenced_column_name 
from information_schema.key_column_usage 
where table_schema = 'task863' 
  and referenced_table_name is not null

***************************************************************************************

--将库中所有多对多表和多对一表都查询出来
select table_name,column_name,referenced_table_name,referenced_column_name
from information_schema.key_column_usage
where referenced_table_name is not null
  and table_schema = 'task863'



  
--查询库中所有只引用了一个表的表的表名
select table_name
from information_schema.key_column_usage 
where table_schema = 'task863' 
  and referenced_table_name is not null
  group by TABLE_NAME having count(distinct referenced_table_name) = 1


--查询库中所有只引用了一个表的引用信息(表名,字段,被引用的表名,对应被引用的字段)
select table_name,column_name,referenced_table_name,referenced_column_name 
from information_schema.key_column_usage 
where referenced_table_name is not null 
  and table_name in 
  (select table_name 
   from information_schema.key_column_usage 
   where table_schema = 'task863' 
     and referenced_table_name is not null 
     group by TABLE_NAME 
     having count(distinct referenced_table_name) = 1)










--查询数据库中所有的实体表的表名
select distinct table_name 
from information_schema.key_column_usage 
where table_schema = 'task863'
  and table_name not in 
     (select table_name 
      from information_schema.key_column_usage 
      where table_schema = 'task863' 
        and referenced_table_name is not null)