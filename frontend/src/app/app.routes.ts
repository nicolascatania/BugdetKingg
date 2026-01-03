import { Routes } from '@angular/router';
import { Home } from './features/home/pages/home/home';
import { Transactions } from './features/transactions/pages/transactions/transactions';
import { Dashboard } from './features/dashboard/pages/dashboard/dashboard';
import { Login } from './features/login/login/login';
import { Register } from './features/login/register/register';
import { MainLayout } from './core/layouts/main-layout/main-layout';
import { AuthLayout } from './core/layouts/auth-layout/auth-layout';

export const routes: Routes = [

    // 🔓 
    {
        path: '',
        component: AuthLayout,
        children: [
            { path: 'login', component: Login },
            { path: 'register', component: Register },
        ]
    },

    // 🔐 
    {
        path: '',
        component: MainLayout,
        // canActivate: [authGuard],
        children: [
            { path: 'home', component: Home },
            { path: 'transactions', component: Transactions },
            { path: 'dashboard', component: Dashboard },
        ]
    },

    // fallback
    { path: '**', redirectTo: 'login' }
];
