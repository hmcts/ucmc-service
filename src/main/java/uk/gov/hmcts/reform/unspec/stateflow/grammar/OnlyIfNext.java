package uk.gov.hmcts.reform.unspec.stateflow.grammar;

/**
 * This specifies what can come after a ONLY_IF clause.
 */
public interface OnlyIfNext<S> extends TransitionTo<S>, State<S>, Subflow<S>, Build {

}

