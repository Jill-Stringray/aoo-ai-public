<template>
  <section>
    <h2>项目分层架构</h2>
    <ul>
      <li>接入层：用户、管理员、JWT 鉴权</li>
      <li>知识层：文档上传、解析、切分、向量化</li>
      <li>检索层：向量检索 + BM25 + Rerank</li>
      <li>生成层：SSE 流式回答与引用溯源</li>
    </ul>
  </section>
</template>
