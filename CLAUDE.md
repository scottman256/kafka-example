# CLAUDE.md

## Coding Style

- Prioritize simple, clean, maintainable, and performant code over clever code.
- Methods should generally be 10 lines or fewer. Exceptions should be rare and justified (e.g. unavoidable branching required by the problem itself, not by poor decomposition).
- If a method grows past ~10 lines, extract well-named helper methods rather than adding a comment to explain a section.
- Name variables, classes, methods, and database fields clearly and precisely — names should make comments unnecessary. Avoid abbreviations unless they are universally understood.
- Avoid premature abstraction. Don't add configurability, indirection, or generic frameworks for hypothetical future needs.
- Avoid unnecessary comments. Code should be self-explanatory through naming and structure; use comments only for non-obvious "why" (e.g. a workaround, a subtle constraint).
- Prefer composition and small, focused classes over large, multi-purpose ones.
- Strict microservices architecture will be used for all Java microservices.

## Testing

- Every method with meaningful logic should have unit test coverage.
- Integration tests should cover each layer boundary (API → Service → Repository → Database).
- Tests should be clean and readable, following the same style principles as production code.
