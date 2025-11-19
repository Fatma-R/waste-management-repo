import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Button } from '../../shared/components/button/button';
import { Card } from '../../shared/components/card/card';

interface FeatureCard {
  icon: string;
  title: string;
  description: string;
}

interface HowItWorksStep {
  icon: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    Button,
    Card
  ],
  templateUrl: './landing.html',
  styleUrls: ['./landing.scss']
})
export class Landing {
  features: FeatureCard[] = [
    {
      icon: '🗺️',
      title: 'Interactive Map',
      description: 'Real-time visualization of all waste bins with precise location tracking and status monitoring.'
    },
    {
      icon: '📊',
      title: 'Real-time Fill Levels',
      description: 'Smart sensors provide instant updates on bin capacity, preventing overflow and optimizing collection.'
    },
    {
      icon: '🚛',
      title: 'Route Optimization',
      description: 'AI-powered algorithms calculate the most efficient collection routes, saving time and fuel.'
    },
    {
      icon: '👥',
      title: 'Employee Management',
      description: 'Assign zones, track performance, and manage your waste collection team effectively.'
    },
    {
      icon: '📈',
      title: 'Analytics & Reports',
      description: 'Comprehensive insights into waste patterns, collection efficiency, and environmental impact.'
    }
  ];

  howItWorksSteps: HowItWorksStep[] = [
    {
      icon: '📡',
      title: 'Smart Sensors',
      description: 'IoT sensors monitor fill levels in real-time and send data to the cloud platform.'
    },
    {
      icon: '🗺️',
      title: 'Live Dashboard',
      description: 'View all bins on an interactive map with color-coded status indicators.'
    },
    {
      icon: '✅',
      title: 'Optimized Routes',
      description: 'System generates efficient collection routes automatically based on fill levels.'
    }
  ];
}
