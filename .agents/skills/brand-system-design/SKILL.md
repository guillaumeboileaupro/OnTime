---
name: brand-system-design
description: Create or review logos, icons, design tokens, and product UI while preserving an existing approved visual identity. Use for branding, logo variants, application icons, UI styling, or visual consistency across platforms.
---

# Brand System Design

Preserve the product's established identity and make visual changes reviewable. Inspect repository instructions, current production assets, design documentation, screenshots, and UI tokens before proposing a change.

## Approval boundary

- Treat production assets as approved unless the user explicitly requests a replacement.
- Keep proposals separate from production files until the user validates one.
- When the user requests one precise adjustment, preserve every unrelated color, proportion, shape, alignment, and export.
- Present the smallest useful set of variants. Label them clearly and wait for validation before integration when approval is required.

## Visual direction

- Prefer simple, modern, flat geometry with solid colors.
- Avoid gradients, decorative shadows, glass effects, bevels, glow, 3D effects, generic AI sparkles, and unnecessary detail.
- Preserve recognition at small sizes and use optical centering, not only mathematical centering.
- Use one clear visual idea rather than combining several symbols.
- Keep typography, spacing, border weight, radii, and colors consistent with the current product.

## Product UI

- Make the main task and current state visually dominant.
- Use one primary action per step and reduce artificial dashboard density.
- Support responsive layouts, visible keyboard focus, touch targets of at least 44 by 44 pixels, and WCAG AA contrast.
- Never communicate state through color alone.
- Include empty, loading, error, disabled, success, and long-content states when relevant.

## Asset delivery

- Keep SVG as the master source for vector marks.
- Derive PNG, ICO, desktop, and Android icons from the approved master rather than redrawing them independently.
- Verify transparent edges, padding, centering, contrast, and legibility at 16, 20, 32, 48, 128, 256, and 512 pixels.
- Reuse the same approved asset across the web UI, desktop package, mobile package, README, and release metadata.
