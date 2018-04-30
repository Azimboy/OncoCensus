package views.html.home

import views.html.helper.{FieldConstructor, FieldElements}

package object simple {
  implicit val simpleField = new FieldConstructor {
    def apply(elts: FieldElements) = simpleFieldConstructor(elts)
  }
}
