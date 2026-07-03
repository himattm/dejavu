## 2024-05-14 - Demo fixtures vs. real optimizations
**Learning:** In a codebase designed to demonstrate or test performance problems (like the `dejavu` library's regression tests), some components are intentionally written to be unoptimized or "unstable" so that testing frameworks have something to catch. Modifying these fixtures breaks the tests that rely on them.
**Action:** Do not blindly optimize components without understanding their purpose. Always verify if an unoptimized component is actually a demo fixture meant to stay unstable before modifying it.
