1\.注意到该chunk异常，之后再进行解决。！！！

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

已解决，同一条 MQ 消息被两个消费端处理了，测试类手动consume一次，自动监听consume一次

5.切分时有部分句子在中间截断导致信息缺失。(目前思路是不同文件格式采用不同的切分方式)！！！

6.RabbitMQ队列异常导致Listener不断执行队列内未完成的操作

已解决，通过查看RabbitMQ控制台，发现是因为前端传入一个不存在的conversationID,导致MQ在消费时插入被Mysql的外键约束阻拦，最终不断反复重试（此时还未添加retry limit)

7.引用完RAG后应当能给一个超链接（退阶：给页码）从而定位文档内容的位置！！！

8.消费异常会无限 requeue
解决方法：
1.入口先校验，不让明显坏消息入队。
2.给 RabbitMQ 队列加 DLQ
3.配置消费端重试和最终拒绝，异常分类---分为可恢复异常和不可恢复异常
以下是四种不可重试的错误
//1.参数非法
//2.状态非法
//3.消息转化失败（IllegalStateException("订单不是待支付状态")，IllegalStateException("消息已经处理")等）
//4.数据完整性异常不重试，比如唯一索引冲突、字段约束失败

9.local_mq_message.retry_count 只统计生产端投递失败！！！

10.业务不可恢复异常没有被提前拦截

加入判断，投递MQ前进行

11.幂等设计不足

修正 outbox event_key，不再用随机 UUID 破坏幂等

12.streamchat()抛出异常无法被SSE捕捉

原因是在创建好SSE之前就抛出了异常。Supplier 的核心作用就一句话： 把一段代码先包起来，晚点再执行。 在这个场景下，它让 SseEmitter 先创建好，再执行可能抛异常的 chatService.streamChat(request)，这样异常就能被 SseService 捕获并按 SSE 格式返回。

13.在召回去重时，数据结构为List<List<Document，如果按照简单的去重，即维护一个hashmap,如果特征不存在则加入，存在则丢弃有可能导致召回分数低的那个可能被去掉。

解决方法：在遍历时加入hashmap时同时记录分数，如果重复，则保留分数高的那个召回项

14.score 和 distance 是互为1的互补关系 例：score=0.7 distance=0.3，正常来说是distance越小越相关，但这里为了方便展示，添加score字段让他按数值越大越好
