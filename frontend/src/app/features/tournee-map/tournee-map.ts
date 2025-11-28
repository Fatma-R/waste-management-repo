import {
  AfterViewInit,
  Component,
  OnDestroy,
  OnInit
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import * as polyline from '@mapbox/polyline';

import { CardComponent } from '../../shared/components/card/card';
import { ButtonComponent } from '../../shared/components/button/button';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner';

import { TourneeService } from '../../core/services/tournee';
import { DepotService } from '../../core/services/depot';
import { CollectionPointService } from '../../core/services/collection-point';
import { TourneeAssignmentService } from '../../core/services/tournee-assignment';

import { Tournee, RouteStep } from '../../shared/models/tournee.model';
import { CollectionPoint } from '../../shared/models/collection-point.model';
import { TrashType } from '../../shared/models/bin.model';
import { TourneeAssignment } from '../../shared/models/tournee-assignment';
import { forkJoin } from 'rxjs';

// -----------------------------------------------------
// Custom Leaflet icons
// -----------------------------------------------------
const binIcon = L.icon({
  iconUrl: 'assets/map/marker-bin.png',
  iconRetinaUrl: 'assets/map/marker-bin.png',
  iconSize: [32, 32],
  iconAnchor: [16, 32],
  popupAnchor: [0, -32]
});

const depotIcon = L.icon({
  iconUrl: 'assets/map/marker-depot.png',
  iconRetinaUrl: 'assets/map/marker-depot.png',
  iconSize: [36, 36],
  iconAnchor: [18, 36],
  popupAnchor: [0, -36]
});

interface StopView {
  step: RouteStep;
  collectionPoint: CollectionPoint;
}

@Component({
  selector: 'app-tournee-map',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    ButtonComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './tournee-map.html',
  styleUrls: ['./tournee-map.scss']
})
export class TourneeMapComponent implements OnInit, AfterViewInit, OnDestroy {
  private map!: L.Map;
  private layerGroup!: L.LayerGroup;

  // Tour state
  isLoading = false;
  tournee: Tournee | null = null;
  stops: StopView[] = [];
  selectedStop: StopView | null = null;

  // Assignment state
  assignments: TourneeAssignment[] = [];
  isAssigning = false;

  // Filters / planning params
  trashTypes = Object.values(TrashType);
  selectedType: TrashType = TrashType.PLASTIC;
  threshold = 80;

  constructor(
    private tourneeService: TourneeService,
    private depotService: DepotService,
    private collectionPointService: CollectionPointService,
    private tourneeAssignmentService: TourneeAssignmentService
  ) {}

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.initMap();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  // ------------------------------------------------------------------
  // Map init
  // ------------------------------------------------------------------
  private initMap(): void {
    this.map = L.map('tourneeMap').setView([36.8, 10.19], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.layerGroup = L.layerGroup().addTo(this.map);
  }

  // ------------------------------------------------------------------
  // Planning
  // ------------------------------------------------------------------
  planTour(): void {
    this.isLoading = true;
    this.tournee = null;
    this.stops = [];
    this.selectedStop = null;
    this.assignments = []; // on réinitialise les affectations
    if (this.layerGroup) {
      this.layerGroup.clearLayers();
    }

    this.tourneeService.planTournee(this.selectedType, this.threshold).subscribe({
      next: (tournee) => {
        this.tournee = tournee;
        this.loadDepotAndCollectionPoints(tournee);
      },
      error: (err) => {
        console.error('Error planning tournee', err);
        alert('Failed to plan tour. Please check backend logs.');
        this.isLoading = false;
      }
    });
  }

  private loadDepotAndCollectionPoints(tournee: Tournee): void {
    const cpIds = [...new Set(tournee.steps.map((s) => s.collectionPointId))];

    if (cpIds.length === 0) {
      this.stops = [];
      this.isLoading = false;
      return;
    }

    const depot$ = this.depotService.getMainDepot();
    const cps$ = forkJoin(
      cpIds.map((id) => this.collectionPointService.getCollectionPointById(id))
    );

    forkJoin({ depot: depot$, cps: cps$ }).subscribe({
      next: ({ depot, cps }) => {
        const cpMap = new Map<string, CollectionPoint>();
        cps.forEach((cp) => {
          if (cp && cp.id) {
            cpMap.set(cp.id, cp);
          }
        });

        const stops: StopView[] = tournee.steps
          .slice()
          .sort((a, b) => a.order - b.order)
          .map((step) => {
            const cp = cpMap.get(step.collectionPointId);
            return cp ? { step, collectionPoint: cp } : null;
          })
          .filter((item): item is StopView => item !== null);

        this.stops = stops;

        if (
          depot.location &&
          depot.location.coordinates &&
          depot.location.coordinates.length === 2
        ) {
          const depotCoords = depot.location.coordinates as [number, number];
          this.renderMap(depotCoords, this.stops);
        }

        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading depot/collection points', err);
        alert('Failed to load depot/collection points.');
        this.isLoading = false;
      }
    });
  }

  // ------------------------------------------------------------------
  // Auto-assign resources for this tour
  // ------------------------------------------------------------------
  assignResources(): void {
    if (!this.tournee) {
      return;
    }

    this.isAssigning = true;

    this.tourneeAssignmentService
      .autoAssignForTournee(this.tournee.id)
      .subscribe({
        next: (assignments) => {
          this.assignments = assignments;
          this.isAssigning = false;
        },
        error: (err) => {
          console.error('Error auto-assigning resources for tournee', err);
          alert('Failed to assign crew and vehicle for this tour.');
          this.isAssigning = false;
        }
      });
  }

  // ---------------- Map rendering (markers + route) ----------------
  private renderMap(
    depotCoords: [number, number],
    stops: StopView[]
  ): void {
    if (!this.layerGroup || !this.map) {
      return;
    }

    this.layerGroup.clearLayers();

    // GeoJSON [lon, lat] -> Leaflet (lat, lon)
    const [depotLon, depotLat] = depotCoords;
    const depotLatLng = L.latLng(depotLat, depotLon);

    // Depot marker
    const depotMarker = L.marker(depotLatLng, {
      title: 'Depot',
      icon: depotIcon
    }).bindPopup('<b>Depot</b>');
    depotMarker.addTo(this.layerGroup);

    // Fallback straight-line route
    const fallbackLatLngs: L.LatLngExpression[] = [depotLatLng];

    // Stops markers
    stops.forEach((stop) => {
      const loc = stop.collectionPoint.location;
      if (!loc || !loc.coordinates || loc.coordinates.length !== 2) {
        return;
      }

      const [lon, lat] = loc.coordinates;
      const latLng = L.latLng(lat, lon);

      fallbackLatLngs.push(latLng);

      const marker = L.marker(latLng, {
        title: `Step ${stop.step.order}`,
        icon: binIcon
      }).bindPopup(
        `<b>Step ${stop.step.order}</b><br>${stop.collectionPoint.adresse || stop.collectionPoint.id}`
      );

      marker.addTo(this.layerGroup);
    });

    if (stops.length > 0) {
      fallbackLatLngs.push(depotLatLng);
    }

    // 1) Try to use VROOM/OSRM geometry if present
    if (this.tournee && this.tournee.geometry) {
      try {
        // VROOM polyline5 => precision 5
        const decoded = polyline.decode(
          this.tournee.geometry,
          5
        ) as [number, number][];

        const geomLatLngs = decoded.map(
          ([lat, lon]: [number, number]) => L.latLng(lat, lon)
        );

        const geometryLine = L.polyline(geomLatLngs, { weight: 4 });
        geometryLine.addTo(this.layerGroup);
        this.map.fitBounds(geometryLine.getBounds(), { padding: [30, 30] });
        return;
      } catch (e) {
        console.error(
          'Error decoding tour geometry polyline, falling back to straight lines',
          e
        );
      }
    }

    // 2) Fallback: straight line depot -> stops -> depot
    const routeLine = L.polyline(fallbackLatLngs, { weight: 4 });
    routeLine.addTo(this.layerGroup);
    this.map.fitBounds(routeLine.getBounds(), { padding: [30, 30] });
  }

  // ---------------- UI helpers ----------------
  selectStop(stop: StopView): void {
    this.selectedStop = stop;

    const loc = stop.collectionPoint.location;
    if (!loc || !loc.coordinates || loc.coordinates.length !== 2) {
      return;
    }

    const [lon, lat] = loc.coordinates;
    const latLng = L.latLng(lat, lon);
    this.map.panTo(latLng);
  }

  closeDetailsPanel(): void {
    this.selectedStop = null;
  }

  getTrashTypeIcon(type: TrashType): string {
    const icons: Record<TrashType, string> = {
      PLASTIC: '♻️',
      ORGANIC: '🍂',
      GLASS: '🥤',
      PAPER: '📄'
    };
    return icons[type] || '🗑️';
  }

  getStatusBadgeClass(_status: string): string {
    return `status-badge status-active`;
  }
}
