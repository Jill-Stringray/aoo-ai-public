import { createRouter, createWebHistory } from 'vue-router';
import OverviewPage from './views/OverviewPage.vue';
import KnowledgePage from './views/KnowledgePage.vue';
import ChatPage from './views/ChatPage.vue';
import AdminPage from './views/AdminPage.vue';
import ProfilePage from './views/ProfilePage.vue';
import LoginPage from './views/LoginPage.vue';

export default createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: OverviewPage,
      meta: { title: '工作台', description: '面向日常使用的知识检索、问答和治理入口。' }
    },
    {
      path: '/knowledge',
      component: KnowledgePage,
      meta: { title: '知识库', description: '沉淀企业文档、维护索引状态并管理知识来源。' }
    },
    {
      path: '/chat',
      component: ChatPage,
      meta: { title: '智能问答', description: '围绕企业知识进行可追溯的业务问答。' }
    },
    {
      path: '/admin',
      component: AdminPage,
      meta: { title: '治理中心', description: '查看系统健康、审计和成本治理信息。' }
    },
    {
      path: '/login',
      component: LoginPage,
      meta: { title: '账号登录', description: '使用企业账号进入 AOO AI 工作空间。' }
    },
    {
      path: '/profile',
      component: ProfilePage,
      meta: { title: '个人中心', description: '查看个人资料与账号状态。' }
    }
  ]
});
