import { createRouter, createWebHistory } from 'vue-router';
import OverviewPage from './views/OverviewPage.vue';
import KnowledgePage from './views/KnowledgePage.vue';
import ChatPage from './views/ChatPage.vue';
import AdminPage from './views/AdminPage.vue';
import ProfilePage from './views/ProfilePage.vue';
import LoginPage from './views/LoginPage.vue';
import { getCurrentIdentity, getHomePathByRole } from './api/auth';

const routes = [
  {
    path: '/login',
    component: LoginPage,
    meta: {
      public: true,
      title: '账号登录',
      description: '登录后按账号身份进入用户端或管理员端。'
    }
  },
  {
    path: '/',
    component: OverviewPage,
    meta: {
      requiresAuth: true,
      section: 'user',
      title: '用户工作台',
      description: '面向普通用户的知识检索、问答和个人效率入口。'
    }
  },
  {
    path: '/chat',
    component: ChatPage,
    meta: {
      requiresAuth: true,
      section: 'user',
      title: '智能问答',
      description: '用户端围绕企业知识进行可追溯的业务问答。'
    }
  },
  {
    path: '/profile',
    component: ProfilePage,
    meta: {
      requiresAuth: true,
      section: 'user',
      title: '个人中心',
      description: '查看个人资料与账号状态。'
    }
  },
  {
    path: '/admin',
    component: AdminPage,
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      section: 'admin',
      title: '管理员控制台',
      description: '管理员端统一处理系统健康、审计、权限和成本治理。'
    }
  },
  {
    path: '/admin/knowledge',
    component: KnowledgePage,
    meta: {
      requiresAuth: true,
      requiresAdmin: true,
      section: 'admin',
      title: '知识库管理',
      description: '管理员端维护企业文档、索引状态和知识来源。'
    }
  },
  {
    path: '/knowledge',
    redirect: '/admin/knowledge'
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const identity = getCurrentIdentity();

  if (to.meta.public) {
    if (identity.authenticated && to.path === '/login') {
      return getHomePathByRole(identity.role);
    }
    return true;
  }

  if (to.meta.requiresAuth && !identity.authenticated) {
    return { path: '/login', query: { redirect: to.fullPath } };
  }

  if (to.meta.requiresAdmin && !identity.isAdmin) {
    return { path: '/', query: { denied: 'admin' } };
  }

  return true;
});

export default router;
