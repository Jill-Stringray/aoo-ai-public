import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './style.css';

createApp(App).use(router).mount('#app');

// Edge bug 临时修复
// 修复最小化弹出的问题
// Edge 浏览器最小化 bug 修复
const originalReplaceState = window.history.replaceState
window.history.replaceState = function (state, title, url) {

    // 如果页面是隐藏状态（最小化）
    if (document.visibilityState === 'hidden') {
        console.log('拦截 replaceState（页面隐藏时）')
        return
    }

    return originalReplaceState.apply(this, arguments)
}