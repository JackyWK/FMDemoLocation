开发记录文档
--
* 消除app标题栏
this.requestWindowFeature(Window.FEATURE_NO_TITLE);
setContentView(R.layout.activity_main);
* 编程规则 消除“魔法数”（难以理解的数字表达式）使用AS自带重构工具即可
* encapsulate field封装字段---建立geter和seter的工具
* 可以用类来封装某些数组