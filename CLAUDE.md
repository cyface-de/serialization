# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

The README is the main documentation. It describes the binary format, the modules, the development commands, the workflows and the release:

@README.md

## Rules for Changes

- Keep the README current. When you change the binary format, a module, a command or a workflow, update the README in the same commit.
- Keep the ZLIB settings (level 5, `nowrap=true`): the iOS SDK cannot change them.
- The binary format is read by other repositories (backend, data collector, tools). A format change needs a new format version.
- Follow the rules of the workflows in the README section "GitHub Workflows" for each change of `.github/workflows/`.

## Commit Style

`[TICKET-ID] Short imperative summary`.
