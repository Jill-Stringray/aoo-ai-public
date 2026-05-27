import { createRouter, createWebHistory } from 'vue-router';
import OverviewPage from './views/OverviewPage.vue';
import KnowledgePage from './views/KnowledgePage.vue';
import ChatPage from './views/ChatPage.vue';
import AdminPage from './views/AdminPage.vue';
import LoginPage from './views/LoginPage.vue';
import ProfilePage from './views/ProfilePage.vue';

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: OverviewPage },
    { path: '/knowledge', component: KnowledgePage },
    { path: '/chat', component: ChatPage },
    { path: '/admin', component: AdminPage },
    { path: '/login', component: LoginPage },
    { path: '/profile', component: ProfilePage }
  ]
});
