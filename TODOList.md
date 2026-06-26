1\.注意到该chunk异常，之后再进行解决

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



2\.在进行召回测试的时候存在一个问题，即重复召回，后续进行改进

同一个 PDF 被导入了很多次，所以向量库里存在：几乎完全相同的 embedding 导致：同一个 chunk 的五份副本



