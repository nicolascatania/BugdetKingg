import { Routes } from '@angular/router';
import { Home } from './features/home/pages/home/home';
import { Dashboard } from './features/dashboard/pages/dashboard/dashboard';
import { Login } from './features/login/login/login';
import { Register } from './features/login/register/register';
import { MainLayout } from './core/layouts/main-layout/main-layout';
import { AuthLayout } from './core/layouts/auth-layout/auth-layout';
import { PublicLayout } from './core/layouts/public-layout/public-layout';
import { Landing } from './features/landing/pages/landing/landing';
import { Terms } from './features/landing/pages/terms/terms';
import { AuthGuard } from './core/guard/auth-guard';
import { TransactionList } from './features/transactions/pages/transaction-list/transaction-list';
import { AccountList } from './features/accounts/pages/account-list/account-list';
import { CategoryList } from './features/categories/pages/category-list/category-list';
import { UserList } from './features/users/pages/user-list/user-list.component';
import { BudgetList } from './features/budgets/pages/budget-list/budget-list';
import { RecurringTransactionList } from './features/recurring-transactions/pages/recurring-transaction-list/recurring-transaction-list';
import { SavingsGoalList } from './features/savings-goals/pages/savings-goal-list/savings-goal-list';

export const routes: Routes = [
  // Public marketing/legal pages: landing at the root, terms next to it.
  {
    path: '',
    component: PublicLayout,
    children: [
      { path: '', component: Landing, pathMatch: 'full' },
      { path: 'terms', component: Terms },
    ],
  },

  {
    path: '',
    component: AuthLayout,
    children: [
      { path: 'login', component: Login },
      { path: 'register', component: Register },
    ],
  },

  {
    path: '',
    component: MainLayout,
    children: [
      { path: 'home', component: Home, canActivate: [AuthGuard] },
      {
        path: 'transactions',
        component: TransactionList,
        canActivate: [AuthGuard],
      },
      { path: 'dashboard', component: Dashboard, canActivate: [AuthGuard] },
      { path: 'accounts', component: AccountList, canActivate: [AuthGuard] },
      { path: 'categories', component: CategoryList, canActivate: [AuthGuard] },
      { path: 'budgets', component: BudgetList, canActivate: [AuthGuard] },
      {
        path: 'recurring-transactions',
        component: RecurringTransactionList,
        canActivate: [AuthGuard],
      },
      { path: 'savings-goals', component: SavingsGoalList, canActivate: [AuthGuard] },
      { path: 'users', component: UserList, canActivate: [AuthGuard] },
    ],
  },

  { path: '**', redirectTo: '' },
];
