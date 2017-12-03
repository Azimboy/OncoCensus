package models.actor_managers

import javax.inject.{Inject, Named}

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.util.Timeout
import models.daos.PatientsDao

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class PatientManager  @Inject()(@Named("patient-manager") val patientManager: ActorRef,
                                 val patientsDao: PatientsDao)
                                (implicit val ec: ExecutionContext)
	extends Actor
		with ActorLogging {

	implicit val defaultTimeout = Timeout(60.seconds)

	override def receive: Receive = ???
}
