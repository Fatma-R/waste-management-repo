// src/app/features/tournee-map/tournee-map.component.ts
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

import { EmployeeService } from '../../core/services/employee';
import { VehicleService } from '../../core/services/vehicle';
import { Employee } from '../../shared/models/employee.model';
import { Vehicle } from '../../shared/models/vehicle.model';

import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BinReadingService } from '../../core/services/bin-reading';

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

const inactiveBinIcon = L.icon({
  iconUrl: 'assets/map/marker-bin-inactive.png',
  iconRetinaUrl: 'assets/map/marker-bin-inactive.png',
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

interface CrewDisplay {
  id: string;
  fullName: string;
  role: string;
}

interface VehicleDisplay {
  id: string;
  plateNumber: string;
  capacityVolumeL: number;
}

interface TourView {
  tournee: Tournee;
  stops: StopView[];
  assignments: TourneeAssignment[];
  crewDisplay: CrewDisplay[];
  vehicleDisplay: VehicleDisplay | null;
  isAssigning: boolean;
}

@Component({
  selector: 'app-tournee-map',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    CardComponent,
    Input,
    ButtonComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './tournee-map.html',
  styleUrls: ['./tournee-map.scss']
})
export class TourneeMapComponent implements OnInit, AfterViewInit, OnDestroy {
  private map!: L.Map;
  private layerGroup!: L.LayerGroup;
  private depotCoords: [number, number] | null = null;

  @Input() tournees: Tournee[] | null = null;
  @Input() collectionPoints: CollectionPoint[] | null = null;

  /** All collection points (active + inactive), always rendered on map */
  allCollectionPoints: CollectionPoint[] = [];

  // Global state
  isLoading = false;
  isAssigning = false; // pour le bouton "Assign crew to all tours"
  hasInitialPoints = false;

  tours: TourView[] = [];
  activeTourIndex: number | null = null;
  selectedStop: StopView | null = null;

  // Toasts
  assignSuccessMessage: string | null = null;
  assignErrorMessage: string | null = null;

  // Filters / planning params
  trashTypes = Object.values(TrashType);
  selectedTypes: TrashType[] = [TrashType.PLASTIC]; // multi-select
  threshold = 80;

  constructor(
    private tourneeService: TourneeService,
    private depotService: DepotService,
    private collectionPointService: CollectionPointService,
    private tourneeAssignmentService: TourneeAssignmentService,
    private employeeService: EmployeeService,
    private vehicleService: VehicleService,
    private binReadingService: BinReadingService
  ) {}

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.initMap();
    this.loadInitialPoints();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
  }

  // Convenience getter: tournée active
  get activeTour(): TourView | null {
    if (this.activeTourIndex == null) {
      return null;
    }
    return this.tours[this.activeTourIndex] || null;
  }

  // Check if a type is currently selected
  isTypeSelected(type: TrashType): boolean {
    return this.selectedTypes.includes(type);
  }

  // Toggle a type on checkbox change
  onWasteTypeToggle(type: TrashType, checked: boolean): void {
    if (checked) {
      if (!this.selectedTypes.includes(type)) {
        this.selectedTypes = [...this.selectedTypes, type];
      }
    } else {
      this.selectedTypes = this.selectedTypes.filter((t) => t !== type);
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

  private loadInitialPoints(): void {
    this.isLoading = true;

    const depot$ = this.depotService.getMainDepot();
    const cps$ = this.collectionPointService.getCollectionPoints();

    forkJoin({ depot: depot$, cps: cps$ }).subscribe({
      next: ({ depot, cps }) => {
        if (
          depot.location &&
          depot.location.coordinates &&
          depot.location.coordinates.length === 2
        ) {
          this.depotCoords = depot.location.coordinates as [number, number];
        }

        this.allCollectionPoints = cps || [];
        this.hasInitialPoints = !!(
          this.allCollectionPoints && this.allCollectionPoints.length
        );

        if (this.depotCoords && this.hasInitialPoints) {
          this.renderInitialMap(this.depotCoords, this.allCollectionPoints);
        } else if (this.depotCoords) {
          const [lon, lat] = this.depotCoords;
          this.map.setView([lat, lon], 12);
        }

        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading initial depot/collection points', err);
        this.isLoading = false;
        // Pas bloquant : la page reste utilisable, mais sans points initiaux
      }
    });
  }

  private renderInitialMap(
    depotCoords: [number, number],
    cps: CollectionPoint[]
  ): void {
    if (!this.layerGroup || !this.map) {
      return;
    }

    this.layerGroup.clearLayers();

    const [depotLon, depotLat] = depotCoords;
    const depotLatLng = L.latLng(depotLat, depotLon);

    // Dépôt
    const depotMarker = L.marker(depotLatLng, {
      title: 'Depot',
      icon: depotIcon
    }).bindPopup('<b>Depot</b>');
    depotMarker.addTo(this.layerGroup);

    const latLngs: L.LatLngExpression[] = [depotLatLng];

    // Tous les points de collecte (actifs + inactifs)
    cps.forEach((cp) => {
      if (
        !cp.location ||
        !cp.location.coordinates ||
        cp.location.coordinates.length !== 2
      ) {
        return;
      }

      const marker = this.createCollectionPointMarker(cp, false);
      marker.addTo(this.layerGroup);

      const [lon, lat] = cp.location.coordinates;
      const latLng = L.latLng(lat, lon);
      latLngs.push(latLng);
    });

    // Fit bounds sur tout
    if (latLngs.length > 1) {
      const bounds = L.latLngBounds(latLngs);
      this.map.fitBounds(bounds, { padding: [30, 30] });
    } else {
      this.map.setView(depotLatLng, 12);
    }
  }

  // ------------------------------------------------------------------
  // Toast helpers
  // ------------------------------------------------------------------
  private showAssignSuccess(message: string): void {
    this.assignSuccessMessage = message;
    this.assignErrorMessage = null;

    setTimeout(() => {
      this.assignSuccessMessage = null;
    }, 3000);
  }

  private showAssignError(message: string): void {
    this.assignErrorMessage = message;
    this.assignSuccessMessage = null;

    setTimeout(() => {
      this.assignErrorMessage = null;
    }, 4000);
  }

  // ------------------------------------------------------------------
  // Plan / discard button handler
  // ------------------------------------------------------------------
  onPlanOrDiscardClick(): void {
    if (this.tours.length) {
      this.discardAllTours();
    } else {
      this.planTours();
    }
  }

  // ------------------------------------------------------------------
  // Planning multiple tours (VROOM) – backend multi-types API
  // ------------------------------------------------------------------
  private planTours(): void {
    // no type selected at all
    if (!this.selectedTypes || !this.selectedTypes.length) {
      this.showAssignError('Please select at least one waste type.');
      return;
    }

    this.isLoading = true;
    this.resetPlanningState(false); // ne touche pas encore à la map

    this.tourneeService
      .planTournees(this.selectedTypes, this.threshold)
      .pipe(
        catchError((err) => {
          console.error('Error planning tours for selected types', err);
          this.isLoading = false;
          this.showAssignError(
            'No tours could be planned for the selected types and threshold.'
          );
          return of([] as Tournee[]);
        })
      )
      .subscribe((allTours: Tournee[]) => {
        if (!allTours.length) {
          this.isLoading = false;
          this.showAssignError(
            'No tours could be planned for the selected types and threshold.'
          );
          return;
        }

        this.loadDepotAndCollectionPointsForTours(allTours);
      });
  }

  private loadDepotAndCollectionPointsForTours(tournees: Tournee[]): void {
    // Récupérer tous les CP uniques sur toutes les tournées
    const cpIdSet = new Set<string>();
    tournees.forEach((t) => {
      t.steps.forEach((s) => {
        if (s.collectionPointId) {
          cpIdSet.add(s.collectionPointId);
        }
      });
    });

    if (!cpIdSet.size) {
      this.tours = [];
      this.isLoading = false;
      return;
    }

    const cpIds = Array.from(cpIdSet);

    const depot$ = this.depotService.getMainDepot();
    const cpsInTours$ = forkJoin(
      cpIds.map((id) => this.collectionPointService.getCollectionPointById(id))
    );

    forkJoin({ depot: depot$, cpsInTours: cpsInTours$ }).subscribe({
      next: ({ depot, cpsInTours }) => {
        // Map id -> CP pour ceux qui sont dans les tournées
        const cpMap = new Map<string, CollectionPoint>();
        cpsInTours.forEach((cp) => {
          if (cp && cp.id) {
            cpMap.set(cp.id, cp);
          }
        });

        // Construire TourView pour chaque tournée
        this.tours = tournees.map((tour) => {
          const stops: StopView[] = tour.steps
            .slice()
            .sort((a, b) => a.order - b.order)
            .map((step) => {
              const cp = cpMap.get(step.collectionPointId);
              return cp ? { step, collectionPoint: cp } : null;
            })
            .filter((item): item is StopView => item !== null);

          const tv: TourView = {
            tournee: tour,
            stops,
            assignments: [],
            crewDisplay: [],
            vehicleDisplay: null,
            isAssigning: false
          };
          return tv;
        });

        // Dépôt
        if (
          depot.location &&
          depot.location.coordinates &&
          depot.location.coordinates.length === 2
        ) {
          this.depotCoords = depot.location.coordinates as [number, number];
        } else {
          this.depotCoords = null;
        }

        this.isLoading = false;
        this.selectedStop = null;

        if (this.tours.length && this.depotCoords) {
          this.activeTourIndex = 0;
          this.renderMap(this.depotCoords, this.tours[0]);
        }
      },
      error: (err) => {
        console.error('Error loading depot/collection points for tours', err);
        this.isLoading = false;
        this.showAssignError('Failed to load depot/collection points.');
      }
    });
  }

  // ------------------------------------------------------------------
  // Discard tours
  // ------------------------------------------------------------------
  private discardAllTours(): void {
    if (!this.tours.length) {
      return;
    }

    const delete$ = this.tours.map((t) =>
      this.tourneeService.deleteTournee(t.tournee.id)
    );

    forkJoin(delete$).subscribe({
      next: () => {
        this.resetPlanningState(true);
        this.showAssignSuccess('All planned tours have been discarded.');
      },
      error: (err) => {
        console.error('Error discarding all tours', err);
        this.showAssignError('Failed to discard all tours.');
      }
    });
  }

  discardTour(tour: TourView, index: number): void {
    this.tourneeService.deleteTournee(tour.tournee.id).subscribe({
      next: () => {
        this.tours.splice(index, 1);

        if (this.tours.length === 0) {
          this.resetPlanningState(true);
          return;
        }

        // Ajuster activeTourIndex
        if (this.activeTourIndex === index) {
          this.selectedStop = null;
          this.activeTourIndex = 0;
          if (this.depotCoords) {
            this.renderMap(this.depotCoords, this.tours[0]);
          }
        } else if (
          this.activeTourIndex !== null &&
          this.activeTourIndex > index
        ) {
          this.activeTourIndex = this.activeTourIndex - 1;
        }
      },
      error: (err) => {
        console.error('Error discarding tour', err);
        this.showAssignError('Failed to discard this tour.');
      }
    });
  }

  private resetPlanningState(clearMap: boolean): void {
    this.tours = [];
    this.activeTourIndex = null;
    this.selectedStop = null;
    this.isAssigning = false;

    if (clearMap && this.layerGroup) {
      this.layerGroup.clearLayers();
      // Revenir à la vue "tous les points" si on a les données
      if (this.depotCoords && this.allCollectionPoints.length) {
        this.renderInitialMap(this.depotCoords, this.allCollectionPoints);
      }
    }
  }

  // ------------------------------------------------------------------
  // Auto-assign resources (crew only, vehicle comes from Tournee)
  // ------------------------------------------------------------------
  assignResourcesForAll(): void {
    if (!this.tours.length) {
      return;
    }

    this.isAssigning = true;

    const assign$ = this.tours.map((t) =>
      this.tourneeAssignmentService.autoAssignForTournee(t.tournee.id)
    );

    forkJoin(assign$).subscribe({
      next: (results) => {
        results.forEach((assignments, idx) => {
          const tour = this.tours[idx];
          this.updateTourAssignments(tour, assignments);
        });
        this.isAssigning = false;
        this.showAssignSuccess('Crew assigned to all tours.');
      },
      error: (err) => {
        console.error('Error assigning resources for all tours', err);
        this.isAssigning = false;
        this.showAssignError('Failed to assign crew for all tours.');
      }
    });
  }

  assignResourcesForTour(tour: TourView, event?: MouseEvent): void {
    // éviter que le clic sur le bouton déclenche aussi le clic sur la carte
    if (event) {
      event.stopPropagation();
    }

    tour.isAssigning = true;

    this.tourneeAssignmentService
      .autoAssignForTournee(tour.tournee.id)
      .subscribe({
        next: (assignments) => {
          this.updateTourAssignments(tour, assignments);
          tour.isAssigning = false;
          this.showAssignSuccess('Crew assigned to this tour.');
        },
        error: (err) => {
          console.error('Error auto-assigning resources for tour', err);
          tour.isAssigning = false;
          this.showAssignError('Failed to assign crew for this tour.');
        }
      });
  }

  private updateTourAssignments(
    tour: TourView,
    assignments: TourneeAssignment[]
  ): void {
    tour.assignments = assignments || [];
    tour.crewDisplay = [];
    tour.vehicleDisplay = null;

    if (!assignments || !assignments.length) {
      return;
    }

    this.loadAssignmentDetailsForTour(tour, assignments);
  }

  private loadAssignmentDetailsForTour(
    tour: TourView,
    assignments: TourneeAssignment[]
  ): void {
    // Vehicle now comes from the Tournee itself (plannedVehicleId),
    // NOT from assignments (we don't set vehicleId there anymore).
    const plannedVehicleId = (tour.tournee as any)
      .plannedVehicleId as string | undefined;

    const employeeIds = Array.from(
      new Set(assignments.map((a) => a.employeeId))
    );
    const employees$ = forkJoin(
      employeeIds.map((id) => this.employeeService.getEmployeeById(id))
    );

    // If there is no planned vehicle, just return null in the forkJoin
    const vehicle$ = plannedVehicleId
      ? this.vehicleService
          .getVehicleById(plannedVehicleId)
          .pipe(
            catchError((err) => {
              console.error('Failed to load planned vehicle', err);
              return of(null as unknown as Vehicle);
            })
          )
      : of(null as unknown as Vehicle);

    forkJoin({ vehicle: vehicle$ , employees: employees$ }).subscribe({
      next: ({ vehicle, employees }) => {
        if (vehicle && (vehicle as any).id) {
          const v = vehicle as Vehicle;
          tour.vehicleDisplay = {
            id: v.id,
            plateNumber: v.plateNumber,
            capacityVolumeL: v.capacityVolumeL
          };
        } else {
          tour.vehicleDisplay = null;
        }

        tour.crewDisplay = employees.map(
          (emp: Employee, index: number): CrewDisplay => {
            const fullName = emp.fullName || emp.id;
            const role = index === 0 ? 'Driver' : 'Collector';
            return { id: emp.id, fullName, role };
          }
        );
      },
      error: (err) => {
        console.error(
          'Failed to load assignment details (employees/vehicle)',
          err
        );
      }
    });
  }

  // ------------------------------------------------------------------
  // Map rendering (markers + route) pour la tournée active
  // ------------------------------------------------------------------
  private renderMap(
    depotCoords: [number, number],
    tour: TourView
  ): void {
    if (!this.layerGroup || !this.map) {
      return;
    }

    this.layerGroup.clearLayers();

    // Dépôt
    const [depotLon, depotLat] = depotCoords;
    const depotLatLng = L.latLng(depotLat, depotLon);

    const depotMarker = L.marker(depotLatLng, {
      title: 'Depot',
      icon: depotIcon
    }).bindPopup('<b>Depot</b>');
    depotMarker.addTo(this.layerGroup);

    const routeColor = this.getRouteColor(tour.tournee.tourneeType);

    // Marqueurs pour TOUS les points de collecte (actifs + inactifs)
    const activeCpIds = new Set<string>(
      tour.stops.map((s) => s.collectionPoint.id)
    );

    const fallbackLatLngs: L.LatLngExpression[] = [depotLatLng];

    this.allCollectionPoints.forEach((cp) => {
      if (
        !cp.location ||
        !cp.location.coordinates ||
        cp.location.coordinates.length !== 2
      ) {
        return;
      }

      const marker = this.createCollectionPointMarker(
        cp,
        activeCpIds.has(cp.id)
      );
      marker.addTo(this.layerGroup);
    });

    // Points de la tournée active pour la route fallback
    tour.stops.forEach((stop) => {
      const loc = stop.collectionPoint.location;
      if (!loc || !loc.coordinates || loc.coordinates.length !== 2) {
        return;
      }
      const [lon, lat] = loc.coordinates;
      const latLng = L.latLng(lat, lon);
      fallbackLatLngs.push(latLng);
    });

    if (tour.stops.length > 0) {
      fallbackLatLngs.push(depotLatLng);
    }

    // 1) Route géométrique VROOM (polyline) si dispo
    if (tour.tournee.geometry) {
      try {
        const decoded = polyline.decode(
          tour.tournee.geometry,
          5
        ) as [number, number][];

        const geomLatLngs = decoded.map(
          ([lat, lon]: [number, number]) => L.latLng(lat, lon)
        );

        const geometryLine = L.polyline(geomLatLngs, {
          weight: 4,
          color: routeColor
        });
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

    // 2) Fallback: lignes droites dépôt -> stops -> dépôt
    const routeLine = L.polyline(fallbackLatLngs, {
      weight: 4,
      color: routeColor
    });
    routeLine.addTo(this.layerGroup);
    this.map.fitBounds(routeLine.getBounds(), { padding: [30, 30] });
  }

  focusTour(index: number, event?: MouseEvent): void {
    if (event) {
      event.stopPropagation();
    }

    if (index < 0 || index >= this.tours.length) {
      return;
    }
    this.activeTourIndex = index;

    if (this.depotCoords) {
      this.renderMap(this.depotCoords, this.tours[index]);
    }
  }

  // ---------------- UI helpers ----------------
  selectStop(tourIndex: number, stop: StopView): void {
    this.activeTourIndex = tourIndex;
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

  getStatusBadgeClass(status: string): string {
    // Tu peux ajuster selon les statuts possibles (PENDING, DONE, etc.)
    switch (status) {
      default:
        return 'status-badge status-active';
    }
  }

  // ------------------------------------------------------------------
  // Helpers pour popups / couleurs / marqueurs
  // ------------------------------------------------------------------
  private getRouteColor(type: TrashType): string {
    switch (type) {
      case TrashType.PLASTIC:
        return '#EFFF00'; // jaune flashy (recyclage plastique)
      case TrashType.ORGANIC:
        return '#854d0e'; // brun/orange (organique)
      case TrashType.PAPER:
        return '#3b82f6'; // bleu (papier/carton)
      case TrashType.GLASS:
        return '#22c55e'; // vert (verre)
      default:
        return '#4b5563'; // gris
    }
  }

  private createCollectionPointMarker(
    cp: CollectionPoint,
    isInActiveTour: boolean
  ): L.Marker {
    const isActive = (cp as any).active !== false; // par défaut true si pas défini
    const icon = isActive ? binIcon : inactiveBinIcon;

    const [lon, lat] = cp.location.coordinates;
    const latLng = L.latLng(lat, lon);

    const marker = L.marker(latLng, {
      title: cp.adresse || cp.id || 'Collection point',
      icon
    });

    // Set a temporary popup while we load latest bin readings
    marker.bindPopup('<div class="cp-popup">Loading bins...</div>');

    // Build the real popup asynchronously using BinReadingService
    this.buildCollectionPointPopup(cp, isInActiveTour, isActive, marker);

    return marker;
  }

  private buildCollectionPointPopup(
    cp: CollectionPoint,
    isInActiveTour: boolean,
    isActive: boolean,
    marker: L.Marker
  ): void {
    const label = cp.adresse || cp.id || 'Collection point';

    // Base container (card style)
    let baseHtml = `<div style="
      font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      font-size: 12px;
      line-height: 1.4;
      max-width: 240px;
      padding: 6px 8px;
    ">`;

    // Title
    baseHtml += `<div style="font-weight: 600; margin-bottom: 4px;">${label}</div>`;

    // Status badges row
    baseHtml += `<div style="display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 6px;">`;

    const statusBg = isActive ? '#DCFCE7' : '#FEE2E2';
    const statusColor = isActive ? '#166534' : '#991B1B';

    baseHtml += `<span style="
      padding: 2px 6px;
      border-radius: 999px;
      font-size: 11px;
      background: ${statusBg};
      color: ${statusColor};
      border: 1px solid rgba(0,0,0,0.06);
    ">Status: ${isActive ? 'Active' : 'Inactive'}</span>`;

    if (isInActiveTour) {
      baseHtml += `<span style="
        padding: 2px 6px;
        border-radius: 999px;
        font-size: 11px;
        background: #DBEAFE;
        color: #1D4ED8;
        border: 1px solid rgba(0,0,0,0.06);
      ">In active tour</span>`;
    }

    baseHtml += `</div>`; // end status row

    const bins = ((cp as any).bins || []) as any[];

    // No bins at this CP
    if (!bins.length) {
      const html =
        baseHtml +
        `<div style="margin-top: 4px; font-size: 11px; color: #6B7280;">No bins</div>` +
        `</div>`;
      marker.setPopupContent(html);
      return;
    }

    // One request per bin to fetch latest reading
    const readingRequests = bins.map((b) =>
      this.binReadingService.getLatestBinReadingForBin(b.id).pipe(
        catchError((err) => {
          console.error(
            'Error loading latest bin reading for bin',
            b.id,
            err
          );
          return of(null);
        })
      )
    );

    forkJoin(readingRequests).subscribe((readings) => {
      const rows: string[] = [];

      bins.forEach((b, index) => {
        const rawType = b.type || b.trashType || b.binType;
        if (!rawType) {
          return;
        }

        const displayType = this.formatTrashTypeLabel(String(rawType));
        const reading = readings[index] as any;

        let fill: number | null = null;
        if (reading != null && reading.fillPct != null) {
          const parsed = Number(reading.fillPct);
          fill = isNaN(parsed) ? null : parsed;
        }

        const value = fill != null ? `${fill.toFixed(0)}%` : 'N/A';

        rows.push(`
          <tr>
            <td style="padding: 2px 4px; white-space: nowrap; color: #374151;">
              ${displayType}
            </td>
            <td style="padding: 2px 4px; text-align: right; font-weight: 500; color: #111827;">
              ${value}
            </td>
          </tr>
        `);
      });

      let html = baseHtml;
      html += `<div style="margin-top: 4px; font-weight: 600;">Bins</div>`;
      html += `<table style="width: 100%; border-collapse: collapse; margin-top: 2px;">`;
      html += rows.join('');
      html += `</table>`;
      html += `</div>`; // close container

      marker.setPopupContent(html);
    });
  }

  private formatTrashTypeLabel(raw: string): string {
    if (!raw) {
      return '';
    }
    const upper = raw.toUpperCase();
    switch (upper) {
      case 'PLASTIC':
        return 'Plastic';
      case 'ORGANIC':
        return 'Organic';
      case 'GLASS':
        return 'Glass';
      case 'PAPER':
        return 'Paper';
      default:
        // Fallback: capitalize first letter, lower the rest
        return raw.charAt(0).toUpperCase() + raw.slice(1).toLowerCase();
    }
  }
}
