package com.yurepires.lazydeploy.exception;

/**
 * Exceção de infraestrutura que impede que detalhes do client HTTP vazem
 * para o domínio da aplicação.
 */
public class ExternalProviderException extends ExternalProviderUnavailableException {

    private final String providerId;
    private final ExternalProviderFailureCategory category;
    private final boolean retryable;

    public ExternalProviderException(
            String providerId,
            ExternalProviderFailureCategory category,
            boolean retryable
    ) {
        this(providerId, category, retryable, null);
    }

    public ExternalProviderException(
            String providerId,
            ExternalProviderFailureCategory category,
            boolean retryable,
            Throwable cause
    ) {
        super("Falha no provider externo: " + providerId);
        this.providerId = providerId;
        if (category == null) {
            this.category = ExternalProviderFailureCategory.UNKNOWN;
        } else {
            this.category = category;
        }
        this.retryable = retryable;
        if (cause != null) {
            initCause(cause);
        }
    }

    public String providerId() {
        return providerId;
    }

    public String getProviderId() {
        return providerId;
    }

    public ExternalProviderFailureCategory category() {
        return category;
    }

    public ExternalProviderFailureCategory getCategory() {
        return category;
    }

    public boolean retryable() {
        return retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
