import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  EventEmitter,
  SimpleChanges,
  viewChild,
} from '@angular/core';
import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import { GraphView } from '../../core/models';

cytoscape.use(dagre);

/**
 * Renders an ownership subgraph.
 * <p>
 * Ownership flows downward: an owner sits above what it owns, so the individual
 * at the top of a chain appears at the top of the picture. A hierarchical
 * (dagre) layout is used rather than a force-directed one because ownership is
 * genuinely a hierarchy, and force layouts turn it into an unreadable cloud.
 */
@Component({
  selector: 'co-graph-canvas',
  standalone: true,
  template: `<div class="canvas" #host></div>`,
  styles: [`
    :host { display: block; }
    .canvas { width: 100%; height: 100%; min-height: 460px; }
  `],
})
export class GraphCanvasComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input({ required: true }) graph!: GraphView;
  @Output() nodeSelected = new EventEmitter<{ id: string; type: string }>();

  private readonly host = viewChild.required<ElementRef<HTMLDivElement>>('host');
  private cy?: cytoscape.Core;

  ngAfterViewInit(): void {
    this.render();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['graph'] && !changes['graph'].firstChange) {
      this.render();
    }
  }

  ngOnDestroy(): void {
    this.cy?.destroy();
  }

  private render(): void {
    const container = this.host()?.nativeElement;
    if (!container || !this.graph) return;

    this.cy?.destroy();

    const nodeIds = new Set(this.graph.nodes.map((n) => n.id));
    const elements: cytoscape.ElementDefinition[] = [
      ...this.graph.nodes.map((n) => ({
        data: {
          id: n.id,
          label: n.label,
          sublabel: n.sublabel,
          kind: n.type,
          flagged: n.flagged ? 'yes' : 'no',
          focus: n.focus ? 'yes' : 'no',
        },
      })),
      // Guard against an edge referencing a node the bounded query did not return.
      ...this.graph.edges
        .filter((e) => nodeIds.has(e.source) && nodeIds.has(e.target))
        .map((e) => ({
          data: {
            id: `${e.source}->${e.target}`,
            source: e.source,
            target: e.target,
            label: e.percentage != null ? `${e.percentage}%` : '',
          },
        })),
    ];

    this.cy = cytoscape({
      container,
      elements,
      minZoom: 0.2,
      maxZoom: 2.5,
      wheelSensitivity: 0.25,
      style: [
        {
          selector: 'node',
          style: {
            label: 'data(label)',
            'font-family': 'Inter, sans-serif',
            'font-size': '11px',
            'font-weight': 500,
            color: '#0f172a',
            'text-valign': 'center',
            'text-halign': 'center',
            'text-wrap': 'wrap',
            'text-max-width': '116px',
            width: 140,
            height: 46,
            shape: 'round-rectangle',
            'background-color': '#ffffff',
            'border-width': 1,
            'border-color': '#cbd5e1',
          },
        },
        {
          selector: 'node[kind = "Person"]',
          style: {
            shape: 'ellipse',
            width: 128,
            height: 62,
            'background-color': '#eff6ff',
            'border-color': '#93c5fd',
          },
        },
        {
          selector: 'node[flagged = "yes"]',
          style: { 'border-color': '#8b5cf6', 'border-width': 2, 'background-color': '#faf8ff' },
        },
        {
          selector: 'node[focus = "yes"]',
          style: {
            'border-color': '#1d4ed8',
            'border-width': 3,
            'background-color': '#dbeafe',
            'font-weight': 700,
          },
        },
        {
          selector: 'edge',
          style: {
            width: 1.4,
            'line-color': '#cbd5e1',
            'target-arrow-color': '#94a3b8',
            'target-arrow-shape': 'triangle',
            'arrow-scale': 0.9,
            'curve-style': 'bezier',
            label: 'data(label)',
            'font-family': 'Inter, sans-serif',
            'font-size': '9.5px',
            color: '#64748b',
            'text-background-color': '#f6f8fb',
            'text-background-opacity': 1,
            'text-background-padding': '2px',
          },
        },
      ],
      layout: {
        name: 'dagre',
        rankDir: 'TB',
        nodeSep: 26,
        rankSep: 62,
        padding: 24,
        fit: true,
      } as cytoscape.LayoutOptions,
    });

    this.cy.on('tap', 'node', (event) => {
      const node = event.target;
      this.nodeSelected.emit({ id: node.id(), type: node.data('kind') });
    });
  }

  fit(): void {
    this.cy?.fit(undefined, 30);
  }
}
