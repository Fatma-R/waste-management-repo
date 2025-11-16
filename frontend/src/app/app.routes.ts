import { Routes } from '@angular/router';
import { HelloPage } from './features/hello/hello-page/hello-page';

export const routes: Routes = [
  { path: '', redirectTo: 'hello', pathMatch: 'full' },
  { path: 'hello', component: HelloPage }
];