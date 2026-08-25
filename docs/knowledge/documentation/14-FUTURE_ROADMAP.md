# Future Roadmap - Planned Enhancements and Phased Approach

## Vision

Evolve from manual block creation to **streamlined generation and management**, enabling faster block creation and consistent quality.

See: [00-HOLISTIC_VISION.md](00-HOLISTIC_VISION.md) for complete vision

## Phase Overview

```
Phase 1: Foundation (Complete)
  ├─ 19 custom blocks
  ├─ Basic CSS styling approach
  ├─ Complete documentation
  └─ Production ready

Phase 2: Enhancement (Weeks 6-10)
  ├─ Additional block variants
  ├─ Component showcase site
  ├─ Advanced tooling
  └─ Team maturity

Phase 3: Automation (Weeks 11-16)
  ├─ Block scaffolding generator
  ├─ Component library management
  └─ Design-to-code assistance

Phase 4: Scale (Future)
  ├─ Multi-site management
  ├─ Team collaboration features
  ├─ Advanced analytics
  └─ Enterprise features
```

## Phase 1: Foundation — Complete

### **Deliverables**
- 19 foundational block specifications
- Basic CSS styling approach (no SCSS; **global** tokens via **`var(--*)`** in **`styles/`**)
- `dataLayer.js` analytics with data-attribute pattern
- `block-helpers.js` extraction utilities
- Complete documentation (16+ files)
- Production reference implementation (text-callout)

### **Architectural Decisions Made**
- ✅ Basic CSS only (no SCSS); **shared** colors / spacing / typography via **CSS variables** in **`styles/`** (**`30-block-css-design-tokens.mdc`**)
- ✅ JSON files with underscore prefix (`_block-name.json`)
- ✅ File names match folder names
- ✅ Data-attribute-driven analytics via `dataLayer.js`
- ✅ Hyphenated class naming (not BEM)

---

## Phase 2: Enhancement (Weeks 6-10)

### **Goals**
- Enhance block system with additional variants
- Create component showcase
- Prepare for automation

### **2.1 Additional Block Variants**

**Hero Block**
- Background image/video support
- Overlay controls
- CTA integration
- Multiple layout options

**Quote Block**
- Testimonial styling
- Attribution options
- Multiple quote styles

**Advanced Form Features**
- Custom validation rules
- Conditional field display
- Multi-step forms

### **2.2 Component Showcase**

- Visual component library (simple HTML pages)
- Interactive variants
- Code examples
- Documentation links

### **2.3 Advanced Tooling**

**Block Scaffolding CLI**
- Generate block folder structure
- Create template JS, CSS, JSON files
- Apply naming conventions automatically
- Include analytics boilerplate

**Testing Tools**
- Visual regression testing
- Accessibility audit tools
- Performance monitoring

---

## Phase 3: Automation (Weeks 11-16)

### **Goals**
- **Streamline block generation**
- **Reduce manual coding effort**

### **3.1 Block Generator Tool**

**Features**:
1. **Scaffolding** - Auto-generate block folder with JS, CSS, JSON
2. **CSS Generation** - Generate basic CSS from design specifications
3. **JSON Model Generation** - Create JSON from field definitions
4. **JavaScript Scaffolding** - Template with extractConfig, buildBlock, etc.

### **3.2 Component Library Management**

- Track component versions
- Release notes
- Update notifications
- Lifecycle management

---

## Phase 4: Scale (Future)

### **Goals**
- Enterprise-grade features
- Multi-site management
- Team collaboration

### **Features**
- Unified dashboard for multiple sites
- Shared block library across sites
- Team management and permissions
- Component usage analytics
- IDE integrations (VS Code extension)
- CLI tools for scaffolding and validation

---

## Success Metrics by Phase

| Phase | Key Metric |
|-------|------------|
| Phase 1 | 19 blocks delivered, docs complete |
| Phase 2 | Component showcase live, additional blocks |
| Phase 3 | 80% reduction in block creation time |
| Phase 4 | Multi-site adoption |

## References

- [00-HOLISTIC_VISION.md](00-HOLISTIC_VISION.md) - Project vision
- [07-FOUNDATIONAL_BLOCKS.md](07-FOUNDATIONAL_BLOCKS.md) - Block list
- [10-PROJECT_STRUCTURE.md](10-PROJECT_STRUCTURE.md) - Repository structure
- [16-BLOCK_DEVELOPMENT_TEMPLATE.md](16-BLOCK_DEVELOPMENT_TEMPLATE.md) - Block template
