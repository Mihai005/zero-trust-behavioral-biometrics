import { createRouter, createWebHistory } from 'vue-router';
import Login from '../components/Login.vue';
import DocumentView from '../components/DocumentView.vue';
import SecureEditor from '../components/SecureEditor.vue';
import { isValidJwt } from '../composables/useAuth';
import Register from '../components/Register.vue';

const getToken = () => sessionStorage.getItem('jwt_token');

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'register',
      component: Register,
      meta: { public: true },
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: DocumentView,
    },
    {
      path: '/secure-editor',
      name: 'secure-editor',
      component: SecureEditor,
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
});

router.beforeEach((to) => {
  if (to.meta.public) {
    return true;
  }

  const token = getToken();

  if (!isValidJwt(token)) {
    if (token) {
      sessionStorage.removeItem('jwt_token');
    }

    return '/login';
  }

  return true;
});

export default router;
