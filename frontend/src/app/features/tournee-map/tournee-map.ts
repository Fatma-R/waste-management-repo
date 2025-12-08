// src/app/features/tournee-map/tournee-map.component.ts
import {
  AfterViewInit,
  Component,
  Input,
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
import {
  CollectionPoint,
  GeoJSONPoint
} from '../../shared/models/collection-point.model';
import { TrashType } from '../../shared/models/bin.model';
import { TourneeAssignment } from '../../shared/models/tournee-assignment';

import { EmployeeService } from '../../core/services/employee';
import { VehicleService } from '../../core/services/vehicle';
import { Employee } from '../../shared/models/employee.model';
import { Vehicle } from '../../shared/models/vehicle.model';

import { forkJoin, of, interval, Subscription, from } from 'rxjs';
import { catchError, concatMap, switchMap, tap } from 'rxjs/operators';
import { BinReadingService } from '../../core/services/bin-reading';
import { AuthService } from '../../core/auth/auth.service';

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
    ButtonComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './tournee-map.html',
  styleUrls: ['./tournee-map.scss']
})
export class TourneeMapComponent implements OnInit, AfterViewInit, OnDestroy {
  isAdmin: boolean = false;

  private map!: L.Map;
  private layerGroup!: L.LayerGroup;
  private vehicleLayer!: L.LayerGroup;
  private depotCoords: [number, number] | null = null;
  private vehicleMarkers = new Map<string, L.Marker>();
  private vehiclesTrackingSub: Subscription | null = null;

  @Input() tournees: Tournee[] | null = null;
  @Input() collectionPoints: CollectionPoint[] | null = null;

  /** All collection points (active + inactive), always rendered on map */
  allCollectionPoints: CollectionPoint[] = [];

  // Global state
  isLoading = false;
  isAssigning = false; // for "Assign crew to all tours"
  hasInitialPoints = false;

  tours: TourView[] = [];
  activeTourIndex: number | null = null;
  selectedStop: StopView | null = null;

  // Toasts
  assignSuccessMessage: string | null = null;
  assignErrorMessage: string | null = null;

  // Admin-only planning params (trash types are NOT filters for employee)
  trashTypes = Object.values(TrashType);
  selectedTypes: TrashType[] = [TrashType.PLASTIC];
  threshold = 80;
  // Admin view mode: list in-progress vs planning new tours
  adminViewMode: 'IN_PROGRESS' | 'PLANNING' = 'IN_PROGRESS';

  constructor(
    private tourneeService: TourneeService,
    private depotService: DepotService,
    private collectionPointService: CollectionPointService,
    private tourneeAssignmentService: TourneeAssignmentService,
    private employeeService: EmployeeService,
    private vehicleService: VehicleService,
    private binReadingService: BinReadingService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
  }

  ngAfterViewInit(): void {
    this.initMap();
    this.loadInitialPoints();
    this.startVehiclesTracking();
  }

  ngOnDestroy(): void {
    if (this.map) {
      this.map.remove();
    }
    this.stopVehiclesTracking();
  }

  // Convenience getter: active tour
  get activeTour(): TourView | null {
    if (this.activeTourIndex == null) {
      return null;
    }
    return this.tours[this.activeTourIndex] || null;
  }

  // Admin-only: check if a type is currently selected
  isTypeSelected(type: TrashType): boolean {
    return this.selectedTypes.includes(type);
  }

  // Admin-only: toggle type when clicking chip
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
    this.vehicleLayer = L.layerGroup().addTo(this.map);
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

        // After base map & collection points are loaded,
        // load initial tours depending on role.
        if (this.isAdmin) {
          this.loadInProgressToursForAdmin();
        } else {
          this.loadInProgressToursForCurrentEmployee();
        }
      },
      error: (err) => {
        console.error('Error loading initial depot/collection points', err);
        this.isLoading = false;
        // Page remains usable, but without initial points
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

    // Depot
    const depotMarker = L.marker(depotLatLng, {
      title: 'Depot',
      icon: depotIcon
    }).bindPopup('<b>Depot</b>');
    depotMarker.addTo(this.layerGroup);

    const latLngs: L.LatLngExpression[] = [depotLatLng];

    // All collection points (active + inactive)
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
  // Admin: Plan / discard button handler
  // ------------------------------------------------------------------
  onPlanOrDiscardClick(): void {
    if (this.tours.length) {
      this.discardAllTours();
    } else {
      this.planTours();
    }
  }

  // ------------------------------------------------------------------
  // Admin: planning tours (VROOM)
  // ------------------------------------------------------------------
  private planTours(): void {
    // no type selected at all
    if (!this.selectedTypes || !this.selectedTypes.length) {
      this.showAssignError('Please select at least one waste type.');
      return;
    }

    this.isLoading = true;
    this.resetPlanningState(false); // do not clear map yet

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

  // ------------------------------------------------------------------
  // Admin: load all in-progress tours
  // ------------------------------------------------------------------
  private loadInProgressToursForAdmin(): void {
    if (!this.isAdmin) {
      return;
    }

    this.adminViewMode = 'IN_PROGRESS';
    this.isLoading = true;

    // Keep base map (all collection points), just reset tours state
    this.resetPlanningState(false);

    this.tourneeService
      .getInProgressTournees()
      .pipe(
        catchError((err) => {
          console.error('Error loading in-progress tours (admin)', err);
          this.isLoading = false;
          return of([] as Tournee[]);
        })
      )
      .subscribe((tours) => {
        if (!tours || !tours.length) {
          // No in-progress tours, keep base map
          this.isLoading = false;
          return;
        }

        this.loadDepotAndCollectionPointsForTours(tours);
      });
  }

  // ------------------------------------------------------------------
  // Employee: load his in-progress tour(s)
  // ------------------------------------------------------------------
  private loadInProgressToursForCurrentEmployee(): void {
    const email = this.authService.getUserEmail();

    if (!email) {
      console.warn(
        'No user email found for current user, skipping in-progress tours load.'
      );
      return;
    }

    // keep the base map (all collection points), just reset tours state
    this.isLoading = true;
    this.resetPlanningState(false);

    // 1) Get the Employee by email (Observable<Employee>)
    this.employeeService.getEmployeeByEmail(email).subscribe({
      next: (employee: Employee) => {
        if (!employee || !employee.id) {
          console.warn(
            'No employee found for email',
            email,
            '— skipping in-progress tours load.'
          );
          this.isLoading = false;
          return;
        }

        const employeeId = employee.id;

        // 2) Now that we have the id, load in-progress tours for this employee
        this.tourneeAssignmentService
          .getInProgressTourneesForEmployee(employeeId)
          .pipe(
            catchError((err) => {
              console.error(
                'Error loading in-progress tours for employee',
                err
              );
              this.isLoading = false;
              return of([] as Tournee[]);
            })
          )
          .subscribe((employeeTours: Tournee[]) => {
            if (!employeeTours || !employeeTours.length) {
              // No current assignment, keep the base map only
              this.isLoading = false;
              return;
            }

            // Reuse your existing logic to build TourView + render map
            this.loadDepotAndCollectionPointsForTours(employeeTours);
          });
      },
      error: (err) => {
        console.error('Error loading employee by email', err);
        this.isLoading = false;
      }
    });
  }

  // ------------------------------------------------------------------
  // Admin: enter planning mode
  // ------------------------------------------------------------------
  startPlanning(): void {
    if (!this.isAdmin) {
      return;
    }

    this.adminViewMode = 'PLANNING';
    this.planTours();
  }

  // ------------------------------------------------------------------
  // Admin: terminate planning and go back to in-progress tours
  // ------------------------------------------------------------------
  terminatePlanning(): void {
    if (!this.isAdmin) {
      return;
    }

    // If nothing is currently planned, just reload in-progress tours
    if (!this.tours.length) {
      this.loadInProgressToursForAdmin();
      return;
    }

    const unassignedTours = this.tours.filter(
      (t) => !t.assignments || !t.assignments.length
    );

    // If everything is assigned, nothing to discard – go straight back
    if (!unassignedTours.length) {
      this.loadInProgressToursForAdmin();
      return;
    }

    const delete$ = unassignedTours.map((t) =>
      this.tourneeService.deleteTournee(t.tournee.id)
    );

    forkJoin(delete$).subscribe({
      next: () => {
        this.showAssignSuccess('Unassigned tours discarded.');
        this.loadInProgressToursForAdmin();
      },
      error: (err) => {
        console.error('Error discarding unassigned tours', err);
        this.showAssignError('Failed to discard some unassigned tours.');
        // Even if some deletions fail, still go back to in-progress view
        this.loadInProgressToursForAdmin();
      }
    });
  }

  private loadDepotAndCollectionPointsForTours(tournees: Tournee[]): void {
    // Collect all CP ids used in these tours
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
        // Map id -> CP for CPs used in tours
        const cpMap = new Map<string, CollectionPoint>();
        cpsInTours.forEach((cp) => {
          if (cp && cp.id) {
            cpMap.set(cp.id, cp);
          }
        });

        // Build TourView for each tour
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

          // If backend ever sends assignments directly on the tour object
          const incomingAssignments = (tour as any)
            .assignments as TourneeAssignment[] | undefined;
          if (incomingAssignments && incomingAssignments.length) {
            this.updateTourAssignments(tv, incomingAssignments);
          }

          return tv;
        });

        // Depot
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

        // Load assignments for each tour (admin IN_PROGRESS or employee view)
        const shouldLoadAssignments =
          (this.isAdmin && this.adminViewMode === 'IN_PROGRESS') ||
          !this.isAdmin;

        if (!shouldLoadAssignments || !this.tours.length) {
          return;
        }

        const assign$ = this.tours.map((tv) =>
          this.tourneeAssignmentService
            .getAssignmentsForTournee(tv.tournee.id)
            .pipe(
              catchError((err) => {
                console.error(
                  'Failed to load assignments for tour',
                  tv.tournee.id,
                  err
                );
                return of([] as TourneeAssignment[]);
              })
            )
        );

        forkJoin(assign$).subscribe((allAssignments) => {
          allAssignments.forEach((assignments, idx) => {
            if (assignments && assignments.length) {
              this.updateTourAssignments(this.tours[idx], assignments);
            }
          });
        });
      },
      error: (err) => {
        console.error('Error loading depot/collection points for tours', err);
        this.isLoading = false;
        this.showAssignError('Failed to load depot/collection points.');
      }
    });
  }

  // ------------------------------------------------------------------
  // Discard tours (admin)
  // ------------------------------------------------------------------
  public discardAllTours(): void {
    if (!this.tours.length) {
      return;
    }

    // Only discard tours that are still unassigned
    const unassignedTours = this.tours.filter(
      (t) => !t.assignments || !t.assignments.length
    );

    if (!unassignedTours.length) {
      this.showAssignError('There are no unassigned tours to discard.');
      return;
    }

    const delete$ = unassignedTours.map((t) =>
      this.tourneeService.deleteTournee(t.tournee.id)
    );

    forkJoin(delete$).subscribe({
      next: () => {
        // Keep only tours that had assignments
        this.tours = this.tours.filter(
          (t) => t.assignments && t.assignments.length
        );

        if (this.tours.length === 0) {
          this.resetPlanningState(true);
        } else if (this.depotCoords) {
          this.activeTourIndex = 0;
          this.selectedStop = null;
          this.renderMap(this.depotCoords, this.tours[0]);
        }

        this.showAssignSuccess('Unassigned tours have been discarded.');
      },
      error: (err) => {
        console.error('Error discarding unassigned tours', err);
        this.showAssignError('Failed to discard unassigned tours.');
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

        // Adjust activeTourIndex
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
      // Back to "all points" view if we have data
      if (this.depotCoords && this.allCollectionPoints.length) {
        this.renderInitialMap(this.depotCoords, this.allCollectionPoints);
      }
    }
  }

  // ------------------------------------------------------------------
  // Auto-assign resources (admin)
  // ------------------------------------------------------------------
  assignResourcesForAll(): void {
  if (!this.tours.length) {
    return;
  }

  // Only assign tours that do not yet have assignments
  const toursToAssign = this.tours.filter(
    (t) => !t.assignments || !t.assignments.length
  );

  if (!toursToAssign.length) {
    this.showAssignError('All tours already have assigned crew.');
    return;
  }

  this.isAssigning = true;

  let successCount = 0;
  let failureCount = 0;
  let hadCapacityError = false; // 👈 track "not enough employees" errors

  from(toursToAssign)
    .pipe(
      concatMap((tour) =>
        this.tourneeAssignmentService
          .autoAssignForTournee(tour.tournee.id)
          .pipe(
            tap((assignments) => {
              if (assignments && assignments.length) {
                this.updateTourAssignments(tour, assignments);
                successCount++;
              } else {
                failureCount++;
              }
            }),
            catchError((err) => {
              console.error(
                'Error assigning resources for tour',
                tour.tournee.id,
                err
              );
              failureCount++;

              // 💡 Try to detect the specific "not enough employees" case
              const backendMsg =
                (err?.error && (err.error.message || err.error)) ||
                err?.message ||
                '';

              if (
                typeof backendMsg === 'string' &&
                backendMsg.includes('Not enough active employees')
              ) {
                hadCapacityError = true;
              }

              // swallow error so the sequence continues to the next tour
              return of([] as TourneeAssignment[]);
            })
          )
      )
    )
    .subscribe({
      next: () => {
        // handled in tap
      },
      error: (err) => {
        console.error('Unexpected error in assignResourcesForAll', err);
        this.isAssigning = false;
        this.showAssignError('Failed to assign crew for all tours.');
      },
      complete: () => {
        this.isAssigning = false;

        if (successCount > 0 && failureCount === 0) {
          this.showAssignSuccess('Crew assigned to all tours.');
        } else if (successCount > 0 && failureCount > 0) {
          if (hadCapacityError) {
            this.showAssignError(
              'Not enough employees to assign all tours. Some tours were assigned, others could not be.'
            );
          } else {
            this.showAssignError(
              'Crew assignment completed with some failures.'
            );
          }
        } else {
          if (hadCapacityError) {
            this.showAssignError(
              'No employees are available to assign these tours.'
            );
          } else {
            this.showAssignError(
              'Failed to assign crew for all selected tours.'
            );
          }
        }
      }
    });
  }


  assignResourcesForTour(tour: TourView, event?: MouseEvent): void {
    if (event) {
      event.stopPropagation();
    }

    // If this tour already has assignments, avoid calling backend again
    if (tour.assignments && tour.assignments.length) {
      this.showAssignError('This tour already has assigned crew.');
      return;
    }

    tour.isAssigning = true;

    this.tourneeAssignmentService
      .autoAssignForTournee(tour.tournee.id)
      .subscribe({
        next: (assignments) => {
          if (assignments && assignments.length) {
            this.updateTourAssignments(tour, assignments);
            this.showAssignSuccess('Crew assigned to this tour.');
          } else {
            this.showAssignError('No crew could be assigned to this tour.');
          }
          tour.isAssigning = false;
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

    const anyTournee = tour.tournee as any;

    if (assignments && assignments.length) {
      // Locally reflect that the tour is now in progress
      if (anyTournee && typeof anyTournee === 'object') {
        anyTournee.status = 'IN_PROGRESS';
      }
      this.loadAssignmentDetailsForTour(tour, assignments);
    } else {
      // No assignments – keep or reset status as planned
      if (anyTournee && typeof anyTournee === 'object') {
        anyTournee.status = anyTournee.status || 'PLANNED';
      }
    }
  }

  private loadAssignmentDetailsForTour(
    tour: TourView,
    assignments: TourneeAssignment[]
  ): void {
    // Prefer the plannedVehicleId from the tour, but if missing,
    // fallback to the vehicle used in the assignments.
    let vehicleId: string | undefined = (tour.tournee as any)
      .plannedVehicleId as string | undefined;

    if (!vehicleId && assignments && assignments.length) {
      vehicleId = assignments[0].vehicleId as string | undefined;
    }

    const employeeIds = Array.from(
      new Set(assignments.map((a) => a.employeeId))
    );

    const employees$ =
      employeeIds.length > 0
        ? forkJoin(
            employeeIds.map((id) => this.employeeService.getEmployeeById(id))
          )
        : of([] as Employee[]);

    const vehicle$ = vehicleId
      ? this.vehicleService.getVehicleById(vehicleId).pipe(
          catchError((err) => {
            console.error('Failed to load vehicle for assignment', err);
            return of(null as unknown as Vehicle);
          })
        )
      : of(null as unknown as Vehicle);

    forkJoin({ vehicle: vehicle$, employees: employees$ }).subscribe({
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
  // Map rendering (markers + route) for active tour
  // ------------------------------------------------------------------
  private renderMap(depotCoords: [number, number], tour: TourView): void {
    if (!this.layerGroup || !this.map) {
      return;
    }

    this.layerGroup.clearLayers();

    const [depotLon, depotLat] = depotCoords;
    const depotLatLng = L.latLng(depotLat, depotLon);

    const depotMarker = L.marker(depotLatLng, {
      title: 'Depot',
      icon: depotIcon
    }).bindPopup('<b>Depot</b>');
    depotMarker.addTo(this.layerGroup);

    const routeColor = this.getRouteColor(tour.tournee.tourneeType);

    // Markers for ALL collection points (active + inactive)
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

    // Points of active tour for fallback route
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

    // 1) Geometric route from VROOM if available
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

    // 2) Fallback: straight lines depot -> stops -> depot
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
    // Adjust according to actual statuses if needed
    switch (status) {
      default:
        return 'status-badge status-active';
    }
  }

  // ------------------------------------------------------------------
  // Helpers for popups / colors / markers
  // ------------------------------------------------------------------
  private getRouteColor(type: TrashType): string {
    switch (type) {
      case TrashType.PLASTIC:
        return '#EFFF00';
      case TrashType.ORGANIC:
        return '#854d0e';
      case TrashType.PAPER:
        return '#3b82f6';
      case TrashType.GLASS:
        return '#22c55e';
      default:
        return '#4b5563';
    }
  }

  private createCollectionPointMarker(
    cp: CollectionPoint,
    isInActiveTour: boolean
  ): L.Marker {
    const isActive = (cp as any).active !== false;
    const icon = isActive ? binIcon : inactiveBinIcon;

    const [lon, lat] = cp.location.coordinates;
    const latLng = L.latLng(lat, lon);

    const marker = L.marker(latLng, {
      title: cp.adresse || cp.id || 'Collection point',
      icon
    });

    marker.bindPopup('<div class="cp-popup">Loading bins...</div>');
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

    let baseHtml = `<div style="
      font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      font-size: 12px;
      line-height: 1.4;
      max-width: 240px;
      padding: 6px 8px;
    ">`;

    baseHtml += `<div style="font-weight: 600; margin-bottom: 4px;">${label}</div>`;

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

    baseHtml += `</div>`;

    const bins = ((cp as any).bins || []) as any[];

    if (!bins.length) {
      const html =
        baseHtml +
        `<div style="margin-top: 4px; font-size: 11px; color: #6B7280;">No bins</div>` +
        `</div>`;
      marker.setPopupContent(html);
      return;
    }

    const readingRequests = bins.map((b) =>
      this.binReadingService.getLatestBinReadingForBin(b.id).pipe(
        catchError((err) => {
          console.error('Error loading latest bin reading for bin', b.id, err);
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
      html += `</div>`;

      marker.setPopupContent(html);
    });
  }

  // ------------------------------------------------------------------
  // Live vehicle tracking for ALL vehicles
  // ------------------------------------------------------------------
  private startVehiclesTracking(): void {
    this.stopVehiclesTracking();

    // Initial fetch
    this.vehicleService
      .getVehicles()
      .pipe(catchError(() => of([] as Vehicle[])))
      .subscribe((vehicles) => this.updateVehicleMarkers(vehicles));

    // Poll every 5 seconds
    this.vehiclesTrackingSub = interval(5000)
      .pipe(
        switchMap(() =>
          this.vehicleService
            .getVehicles()
            .pipe(catchError(() => of([] as Vehicle[])))
        )
      )
      .subscribe((vehicles) => this.updateVehicleMarkers(vehicles));
  }

  private stopVehiclesTracking(): void {
    if (this.vehiclesTrackingSub) {
      this.vehiclesTrackingSub.unsubscribe();
      this.vehiclesTrackingSub = null;
    }
    if (this.vehicleLayer) {
      this.vehicleMarkers.forEach((marker) =>
        this.vehicleLayer.removeLayer(marker)
      );
    }
    this.vehicleMarkers.clear();
  }

  private updateVehicleMarkers(vehicles: Vehicle[]): void {
    if (!this.map || !this.vehicleLayer || !vehicles) {
      return;
    }

    const seen = new Set<string>();

    vehicles.forEach((v) => {
      if (
        !v.id ||
        !v.currentLocation ||
        !v.currentLocation.coordinates ||
        v.currentLocation.coordinates.length !== 2
      ) {
        return;
      }

      const [lon, lat] = v.currentLocation.coordinates;
      const latLng = L.latLng(lat, lon);
      seen.add(v.id);

      let marker = this.vehicleMarkers.get(v.id);
      if (!marker) {
        const vehicleIcon = L.icon({
          iconUrl: 'assets/map/truck.png',
          iconSize: [36, 36],
          iconAnchor: [18, 18]
        });
        marker = L.marker(latLng, {
          icon: vehicleIcon,
          title: v.plateNumber || 'Vehicle'
        }).bindPopup(
          `<div style="font-weight:600">${v.plateNumber || 'Vehicle'}</div>
           <div style="font-size:12px;color:#334155">${v.status || ''}</div>`
        );
        marker.addTo(this.vehicleLayer);
        this.vehicleMarkers.set(v.id, marker);
      } else {
        marker.setLatLng(latLng);
      }
    });

    // Remove markers for vehicles no longer present
    this.vehicleMarkers.forEach((marker, id) => {
      if (!seen.has(id)) {
        this.vehicleLayer.removeLayer(marker);
        this.vehicleMarkers.delete(id);
      }
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
        return raw.charAt(0).toUpperCase() + raw.slice(1).toLowerCase();
    }
  }
}
