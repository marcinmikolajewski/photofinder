package eu.mm.software.photofinder.user.domain;

import java.util.Set;


public interface UserFormAttributes {

	String getId();

	String getUserName();

	String getUserEmail();

	String getPasswordForm1();
	
	String getPasswordForm2();

	Boolean getActive();

	Set<String> getRoles();

}
