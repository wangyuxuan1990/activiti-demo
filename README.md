# activiti-demo

运行activiti 的 API时，打印出来的部分SQL语句 总出现 两个 ORDER BY （select ... from table order by order by id 类似），
Activiti 5.18 的Mybatis版本问题，将mybatis版本从3.3.1 换为3.2.5后测试通过。