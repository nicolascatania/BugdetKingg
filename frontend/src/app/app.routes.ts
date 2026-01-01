import { Routes } from '@angular/router';
import { Home } from './features/home/pages/home/home';
import { Transactions } from './features/transactions/pages/transactions/transactions';
import { Dashboard } from './features/dashboard/pages/dashboard/dashboard';

export const routes: Routes = [

    { path: '', redirectTo: '/home', pathMatch: 'full' },
    { path: 'home', component: Home },
    { path: 'transactions', component: Transactions },
    { path: 'dashboard', component: Dashboard },
    { path: '**', redirectTo: '/home' },
];
