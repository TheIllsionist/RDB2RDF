/*
  查找一个库中有哪些实体表
 */
select table_name
from information_schema.key_column_usage
where table_schema = 'TestDB'
  and table_name not in
		(select table_name
	 	 from information_schema.key_column_usage
	 	 where table_schema = 'TestDB'
	  	 and referenced_table_name is not null)

/*
  查找一个库中的
 */
