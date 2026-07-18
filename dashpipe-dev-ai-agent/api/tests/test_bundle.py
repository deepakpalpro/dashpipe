from app.platform.bundle import proposed_to_bundle
from app.schemas import ProposedPipeline, ProposedStep


def test_proposed_to_bundle_shape():
    proposed = ProposedPipeline(
        name="Test Pipeline",
        description="demo",
        steps=[
            ProposedStep(
                pipelet_id="plet-manual-source",
                step_order=1,
                connector_ids=["ct-rest::My API"],
            ),
            ProposedStep(pipelet_id="plet-s3-destination", step_order=2),
        ],
    )
    bundle = proposed_to_bundle(proposed)
    assert bundle["format_version"] == "1"
    assert bundle["pipeline"]["name"] == "Test Pipeline"
    assert len(bundle["steps"]) == 2
    assert bundle["steps"][0]["connector_refs"] == ["ct-rest::My API"]
    assert bundle["connectors"] == []
    assert bundle["services"] == []
