package com.heavens_above

/**
 * Defines a field which is a global unique identifier for a particular type.
 * Entities which are exposed as GraphQL types should implement this to ensure that caching functions properly,
 * when Apollo in the frontend.
 **/
trait Identifiable {
  def id: String
}
