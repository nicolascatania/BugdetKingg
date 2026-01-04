import { Routes } from '@angular/router';
import { Home } from './features/home/pages/home/home';
import { Dashboard } from './features/dashboard/pages/dashboard/dashboard';
import { Login } from './features/login/login/login';
import { Register } from './features/login/register/register';
import { MainLayout } from './core/layouts/main-layout/main-layout';
import { AuthLayout } from './core/layouts/auth-layout/auth-layout';
import { AuthGuard } from './core/guard/auth-guard';
import { Accounts } from './features/home/components/accounts/accounts';
import { TransactionList } from './features/transactions/pages/transaction-list/transaction-list';
import { AccountList } from './features/accounts/pages/account-list/account-list';

export const routes: Routes = [

    { path: '', redirectTo: 'login', pathMatch: 'full' },

    {
        path: '',
        component: AuthLayout,
        children: [
            { path: 'login', component: Login },
            { path: 'register', component: Register },
        ]
    },

    {
        path: '',
        component: MainLayout,
        children: [
            { path: 'home', component: Home, canActivate: [AuthGuard] },
            { path: 'transactions', component: TransactionList, canActivate: [AuthGuard] },
            { path: 'dashboard', component: Dashboard, canActivate: [AuthGuard] },
            { path: 'accounts', component: AccountList, canActivate: [AuthGuard] },
        ]
    },

    { path: '**', redirectTo: 'login' }
];
