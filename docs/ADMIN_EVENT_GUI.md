# Event Admin GUI Guide

This guide documents the in-game admin interface for global event control.

## Access

- Permission required: `avertoxquest.admin`
- Open with:
  - `/event`
  - `/event gui`

## Main Console (Event Control Center)

Purpose:
- Monitor active/upcoming event runtime
- Start/stop events quickly
- Browse all loaded event definitions

Card interactions:
- Left click: start selected event with broadcast
- Shift + left click: start selected event silently (testing mode)
- Right click: open detail controls for that specific event

Bottom controls:
- Previous / next page
- Stop active event
- Trigger random enabled event
- Refresh dashboard

## Detail Panel

Purpose:
- Inspect one event deeply before triggering

Shows:
- Event ID, description, duration, enabled flag
- Last trigger timestamp
- Next scheduler trigger ETA
- Active remaining time
- Effect list and values
- Placeholder/status preview text

Controls:
- Start (broadcast)
- Start (silent)
- Stop active event
- Back to console page
- Refresh detail data

## Operational Tips

- Use silent starts for QA/testing flow.
- Use broadcast starts for production live events.
- If no events appear, verify definition files and run `/quest reload`.
- If scheduler seems idle, verify `events.scheduler.enabled` and interval settings in config.
