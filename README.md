# islewars-plugin


## Automatic deployment

After Maven verification succeeds on a push to `main` (including merged pull
requests), the workflow triggers the Minecraft image build and VPS rollout in
`Minedesso/minedesso-infra`. Set the repository secret `INFRA_DISPATCH_TOKEN` to a
fine-grained token with Contents read/write access to that infrastructure
repository. PR, tag and development builds do not trigger deployment.

The infrastructure workflow must first be merged into its default `main` branch
and its production SSH secrets configured. See the infrastructure repository's
[deployment setup](https://github.com/Minedesso/minedesso-infra/blob/main/DEPLOYMENT.md).
