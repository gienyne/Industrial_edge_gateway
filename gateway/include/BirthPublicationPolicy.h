#ifndef BIRTHPUBLICATIONPOLICY_H
#define BIRTHPUBLICATIONPOLICY_H

/**
 * @brief Determines whether the Edge Node is allowed to publish Birth messages.
 * 
 * In the current version, Birth publication is always allowed because no
 * Primary Host Application is configured.
 * 
 * This function provides a dedicated decision point for the Birth publication
 * policy. If Host Application state management is introduced in the future,
 * the corresponding logic can be added here without changing the components
 * that trigger Birth publication.
 * 
 * @return true if Birth messages may be published.
 */
inline bool isAllowedToPublishBirth(){
    return true;
}

#endif