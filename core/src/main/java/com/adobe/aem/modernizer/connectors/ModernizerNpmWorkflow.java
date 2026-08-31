package com.adobe.aem.modernizer.connectors;

/**
 * Combined EDS {@code Build} workflow ({@code .github/workflows/main.yaml}).
 * Push runs {@code lint}. Dashboard Heal CI / npm buttons dispatch {@code lint:fix}
 * or {@code build:json} on the same job and commit the result.
 */
public final class ModernizerNpmWorkflow {

    private ModernizerNpmWorkflow() {}

    public static final String YAML =
            "name: Build\n"
            + "on:\n"
            + "  push:\n"
            + "  workflow_dispatch:\n"
            + "    inputs:\n"
            + "      command:\n"
            + "        description: 'npm script (lint, lint:fix, or build:json). On push this defaults to lint.'\n"
            + "        required: false\n"
            + "        default: 'lint'\n"
            + "permissions:\n"
            + "  contents: write\n"
            + "  actions: read\n"
            + "jobs:\n"
            + "  build:\n"
            + "    runs-on: ubuntu-latest\n"
            + "    steps:\n"
            + "      - uses: actions/checkout@v6\n"
            + "      - name: Use Node.js\n"
            + "        uses: actions/setup-node@v6\n"
            + "        with:\n"
            + "          node-version: 24\n"
            + "          cache: npm\n"
            + "      - name: Install\n"
            + "        run: npm ci || npm install\n"
            + "      - name: Run npm script\n"
            + "        run: |\n"
            + "          CMD=\"${{ github.event.inputs.command }}\"\n"
            + "          if [ -z \"$CMD\" ]; then CMD=lint; fi\n"
            + "          npm run \"$CMD\"\n"
            + "      - name: Commit lint:fix or build:json output\n"
            + "        if: github.event_name == 'workflow_dispatch' && (github.event.inputs.command == 'lint:fix' || github.event.inputs.command == 'build:json')\n"
            + "        run: |\n"
            + "          git config user.name \"aem-eds-modernizer\"\n"
            + "          git config user.email \"modernizer@users.noreply.github.com\"\n"
            + "          git add -A\n"
            + "          if git diff --staged --quiet; then\n"
            + "            echo \"No changes to commit.\"\n"
            + "          else\n"
            + "            git commit -m \"chore: apply ${{ github.event.inputs.command }}\"\n"
            + "            git push\n"
            + "          fi\n";
}
