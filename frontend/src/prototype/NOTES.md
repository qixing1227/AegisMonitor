# AegisMonitor Dashboard Prototype

Question: Which first-screen monitoring console layout should we carry into the real Vue frontend?

Plan: Three variants of the dashboard page, switchable via `?variant=A|B|C`, on the throwaway `/prototype/dashboard` route.

Verdict placeholder:

- Winning direction: Variant C - Host Workbench.
- Parts to keep: host-first navigation, compact host rail/table, selected-host detail area, clear real/demo labeling.
- Parts to discard: prototype switcher, sample-data fallback in production UI, alternate A/B layouts after FE-0001 is absorbed.
