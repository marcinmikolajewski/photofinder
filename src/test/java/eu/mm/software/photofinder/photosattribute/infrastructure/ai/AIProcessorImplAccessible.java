package eu.mm.software.photofinder.photosattribute.infrastructure.ai;

class AIProcessorImplAccessible extends AIProcessorImpl {
    // Provide a package-visible bridging constructor for tests that matches the production signature
    AIProcessorImplAccessible(java.util.List<AbstractAiService> allAiServices) {
        super(allAiServices);
    }
}
