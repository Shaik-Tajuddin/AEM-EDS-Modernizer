package com.adobe.aem.modernizer.connectors;

/**
 * GitHub Actions workflow dispatched from the dashboard to run EDS npm scripts
 * ({@code lint:fix}, {@code build:json}) on the preview branch.
 */
public final class ModernizerNpmWorkflow {

    private ModernizerNpmWorkflow() {}

    public static final String YAML =
            "name: modernizer-npm\n"
            + "on:\n"
            + "  workflow_dispatch:\n"
            + "    inputs:\n"
            + "      command:\n"
            + "        description: 'npm script to run (lint:fix or build:json)'\n"
            + "        required: true\n"
            + "        default: 'lint:fix'\n"
            + "permissions:\n"
            + "  contents: write\n"
            + "  actions: read\n"
            + "jobs:\n"
            + "  npm:\n"
            + "    runs-on: ubuntu-latest\n"
            + "    steps:\n"
            + "      - uses: actions/checkout@v4\n"
            + "      - uses: actions/setup-node@v4\n"
            + "        with:\n"
            + "          node-version: '20'\n"
            + "          cache: npm\n"
            + "      - name: Install\n"
            + "        run: npm ci --legacy-peer-deps || npm install --legacy-peer-deps\n"
            + "      - name: Run npm script\n"
            + "        run: npm run \"${{ github.event.inputs.command }}\"\n"
            + "      - name: Commit lint:fix or build:json output\n"
            + "        if: github.event.inputs.command == 'build:json' || github.event.inputs.command == 'lint:fix'\n"
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
