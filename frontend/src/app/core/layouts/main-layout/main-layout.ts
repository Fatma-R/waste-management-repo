import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { Header } from '../../../shared/layout/header/header';
import { Footer } from '../../../shared/layout/footer/footer';

@Component({
  selector: 'app-main-layout',
  imports: [RouterModule, Header, Footer],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout {

}
