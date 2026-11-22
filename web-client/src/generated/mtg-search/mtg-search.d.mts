type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function compileQuery(query: string): CompiledQuery;
export declare function isAdvancedQuery(query: string): boolean;
export declare function searchKeys(): Array<SearchKey>;
export declare class SearchKey {
    constructor(aliases: Array<string>, ops: Array<string>, description: string);
    get aliases(): Array<string>;
    get ops(): Array<string>;
    get description(): string;
}
export declare namespace SearchKey {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => SearchKey;
    }
}
export declare class CompiledQuery {
    private constructor();
    get errors(): Array<QueryError>;
    get isEmpty(): boolean;
    get isAdvanced(): boolean;
    get setFilter(): Nullable<string>;
    get explanation(): Nullable<string>;
    get constrainsName(): boolean;
    get isConjunction(): boolean;
    get topLevelTermSpans(): Int32Array;
    matches(card: any): boolean;
    matchedPrintedName(card: any): Nullable<string>;
}
export declare namespace CompiledQuery {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => CompiledQuery;
    }
}
export declare class QueryError {
    constructor(message: string, start: number, end: number, suggestion: Nullable<string>, fix: Nullable<QueryFix>);
    get message(): string;
    get start(): number;
    get end(): number;
    get suggestion(): Nullable<string>;
    get fix(): Nullable<QueryFix>;
}
export declare namespace QueryError {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => QueryError;
    }
}
export declare class QueryFix {
    constructor(start: number, end: number, text: string);
    get start(): number;
    get end(): number;
    get text(): string;
}
export declare namespace QueryFix {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => QueryFix;
    }
}