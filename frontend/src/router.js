import { createRouter, createWebHistory } from 'vue-router';
import AuthPage from './views/AuthPage.vue';
import ChatPage from './views/ChatPage.vue';
import HomePage from './views/HomePage.vue';
import ProfilePage from './views/ProfilePage.vue';
import RoleWorkshopPage from './views/RoleWorkshopPage.vue';

const routes = [
  { path: '/auth', name: 'auth', component: AuthPage },
  { path: '/', name: 'home', component: HomePage },
  { path: '/chat', name: 'chat', component: ChatPage },
  { path: '/workshop', name: 'workshop', component: RoleWorkshopPage },
  { path: '/profile', name: 'profile', component: ProfilePage }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
