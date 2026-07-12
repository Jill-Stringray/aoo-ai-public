1\.注意到该chunk异常，之后再进行解决。

t .
i S
re e
l e
y ?
v
alid gh
t o
i s
e t
s
. p
S o
h s
o s
w ib
s il
o i
m -
该 chunk 内容长度为：491

2\.在进行召回测试的时候存在一个问题，即重复召回，后续进行改进。

问题描述：同一个 PDF 被导入了很多次，所以向量库里存在：几乎完全相同的 embedding 导致：同一个 chunk 的五份副本。

第一次解决问题：已修复多次导入污染向量库的内容，通过检测contentHash内容是否相同来去重。

3.在进行召回时，发现短的，无意义的向量被存入向量库里。

已解决，通过限制短长度向量入库提高检索质量。

4.在测试类执行上传入库这个功能时，发现vector_store会有两份一模一样的embedding。

已解决，MQ没有幂等导致的，测试类手动consume一次，自动监听consume一次

5.切分时有部分句子在中间截断导致信息缺失。(目前思路是不同文件格式采用不同的切分方式)

6.RabbitMQ队列异常导致Listener不断执行队列内未完成的操作

已解决，通过查看RabbitMQ控制台，发现是因为前端传入一个不存在的conversationID,导致MQ在消费时插入被Mysql的外键约束阻拦，最终不断反复重试（此时还未添加retry limit)
